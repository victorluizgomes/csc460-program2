import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/*=============================================================================
|   Assignment:  Program #2 (Prog2B.java)
|       Author:  Victor Gomes (victorluizgomes@email.arizona.edu)
|
|       Course:  CSc 460 Spring 2018
|   Instructor:  L. McCann
|     Due Date:  February 8th, at the beginning of class
|
|     Language:  Java 1.8
|  Compile/Run:  When running this program, supplement it with an .bin (binary)
|				  file as an argument, which will be used to create the .idx (index) file.
|
+-----------------------------------------------------------------------------
|
|  Description:  The program takes a .bin file that will be read line by line, which will be 
|				  used to create a index file that will store those records from the .bin file.
|				  The index file will contain buckets that are 50 in size that will hold a 
|				  CPSC case # that is hashed to the correct bucket and a Location number, 
|				  which is the location of that CPSC # in the .bin file (database file).
|                
|        Input:  A binary file that was created in Prog2A as the command line argument.
|
|       Output:  A summary of the index file created including (a) the number of buckets in the
|				  index, (b) the number of records in the lowest occupancy bucket, (c) the number
|				  of records in the highest occupancy bucket and (d) the mean of occupancies.
|
|   Techniques:  I created a custom Data Structure for the buckets which has 50 Fields and
|				  some useful methods (more info about it on the class itself). The Fields hold
|				  a CPSC number and a location number.
|
|   Known Bugs:  None
|
*===========================================================================*/

public class Prog2B {
	public static void main(String[] args) {
		
		AllDataRecords records; // to store the records 
		
		RandomAccessFile dataStream = null; 	// the database stream to be read
		RandomAccessFile indexStream = null;	// the index stream to be written
		File fileRef = null;					// to get the file from the arguments
		File idxFile = null;					// to create the file for index
		
		int h = 0; 				// The number of rehashings for the hash function
		
		int strRecordsLen = 0;  // the length in bytes of all the string fields
		int totalLineLen = 0;   // the whole line length
		long numOfRecords = 0;  // the number of records in the file
		
		int lowBucket = 51;     // lowest occupancy bucket (never more than 51)
		int highBucket = 0;	    // highest occupancy bucket
		double meanBucket = 0;     // mean of occupancies for all buickets
		
		try {
			
			// File to read CSCP #'s from Prog2A
			fileRef = new File(args[0]);
			dataStream = new RandomAccessFile(fileRef, "rw");
			
			// the index file to be written and read from
			idxFile = new File("simplelinear.idx");
			indexStream = new RandomAccessFile(idxFile, "rw");
			records = new AllDataRecords();
			
			// Read the last 6 ints containing the lengths of the string fields (And number of records)
			dataStream.seek(dataStream.length() - 28);
			records.setDateLength(dataStream.readInt());
			records.setStratumLength(dataStream.readInt());
			records.setRaceOtherLength(dataStream.readInt());
			records.setDiagOtherLength(dataStream.readInt());
			records.setNarr1Length(dataStream.readInt());
			records.setNarr2Length(dataStream.readInt());
			records.setNumOfRecords(dataStream.readInt());
			
			// Combine the lengths of the string records
			strRecordsLen = records.getDateLength() + records.getStratumLength() + 
					records.getRaceOtherLength() + records.getDiagOtherLength() + 
					records.getNarr1Length() + records.getNarr2Length();
			
			// Sum of the string records + 12 ints (4 bytes) + 1 double (8 bytes) to get line length
			totalLineLen = strRecordsLen + (12 * 4) + 8;
			
			// Calculate the amount of records in the file
			numOfRecords = records.getnumOfRecords(); 
			
			// Creates the 2 new starting buckets
			Bucket bucket = new Bucket();
			writeIndex(indexStream, bucket, 0);
			writeIndex(indexStream, bucket, 400); // 50 * 2 ints(4) = 400
			int index = -1;
			
			// go through all records
			dataStream.seek(0);
			for(int i = 1; i <= numOfRecords; i++) {
				records.readBinary(dataStream); // reads all fields of a line
				
				// Gets in which bucket we should put the Cpsc #
				index = hash(records.getCpscCase(), h);  
					
				// read the bucket in the index location from the hash function
				bucket = readIndex(indexStream, index * 400);
				
				// Rehashing
				if(bucket.isFilled()) {
					rehash(indexStream, h);
					h = h + 1; // Increase number of rehashings for the hash function
					
					// calculate hash and read bucket again
					index = hash(records.getCpscCase(), h); 
					bucket = readIndex(indexStream, index * 400);
				} 
				
				// append the records to the current bucket
				bucket.append(records.getCpscCase(), totalLineLen * (i - 1));
				
				// write the bucket back to the index file
				writeIndex(indexStream, bucket, index * 400);
			}
			
			// Goes through the final buckets to set the final summary
			for(int i = 0; i < (int)Math.pow(2, h + 1); i++) {
				
				bucket = readIndex(indexStream, i * 400);
				// Set the lowest bucket
				if(bucket.actualLength() < lowBucket) {
					lowBucket = bucket.actualLength();
				}
				
				// Sets the highest bucket
				if(bucket.actualLength() > highBucket) {
					highBucket = bucket.actualLength();
				}
				
				// sum of all records
				meanBucket = meanBucket + bucket.actualLength();
			}
			
			// Mean bucket is the sum of all the records in all buckets divided by number of buckets
			meanBucket = meanBucket / Math.pow(2, h + 1);
			
			// Final summary of index file
			System.out.println("-----------------------------------------------");
			System.out.println("Summary of the Simplified Linear Hashing index: ");
			System.out.println("-----------------------------------------------");
			System.out.println("Number of Buckets in file: " + (int)Math.pow(2, h + 1));
			System.out.println("Number of records in the lowest occupancy bucket: " + lowBucket);
			System.out.println("Number of records in the highest occupancy bucket: " + highBucket);
			System.out.println("Mean of occupancies for all buckets: " + meanBucket);
			
			// Write H value (number of rehashings to the end of the index file)
			indexStream.seek(indexStream.length());
			indexStream.writeInt(h);
			
			indexStream.close();
			dataStream.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*-------------------------------------------------------------------
    |  Method hash (Prog2B)
    |
    | 		 Purpose:  Hash function to determine in which bucket the number
    |					given should go to. number % number^(H + 1)
    |
    |  Pre-condition:  A correct cpsc number must be given
    |
    | Post-condition:  -
    |
    |     Parameters:  1. int number -- the number to be hashed on hash function
    |				   2. int h -- the number of rehashs in the file
    |      	
    |        Returns:  Returns the calculated index for the cpsc number
    |
    *-------------------------------------------------------------------*/
	public static int hash(int number, int h) {
		
		return number % (int)Math.pow(2, h + 1);
	}
	
	/*-------------------------------------------------------------------
    |  Method rehash (Prog2B)
    |
    | 		 Purpose:  Takes a full bucket from the binary file and creates 2 
    |					buckets out of it that are rehashed using the hash function.
    |
    |  Pre-condition:  Determine if bucket is full and should be rehashed
    |
    | Post-condition:  -
    |
    |     Parameters:  1. RandomAccessFile stream -- the stream to be read and written to
    |				   2. int h -- the number of rehashs in the file
    |      	
    |        Returns:  -
    |
    *-------------------------------------------------------------------*/
	private static void rehash(RandomAccessFile stream, int h) {
		
		// Sets the current Bucket and the 2 new Buckets
		Bucket currBuck = null;
		Bucket buck1 = null;
		Bucket buck2 = null;
		
		int index = -1;
		int numOfBuckets = (int)Math.pow(2, h + 1);
		
		// Go through all the buckets to double number of buckets one by one
		for(int i = 0; i < numOfBuckets; i++) {
		
			buck1 = new Bucket();
			buck2 = new Bucket();
			currBuck = readIndex(stream, i * 400);
			
			// go through all elements of the current bucket and rehash them to 2 buckets
			for(int j = 0; j < currBuck.actualLength(); j++) {
				
				// if the number is a place holder do not append
				if(currBuck.get(j).getCpscNum() != -1) {
					index = hash(currBuck.get(j).getCpscNum(), h + 1);
					
					// determines which of the 2 buckets to append to
					if(index == i) {
						buck1.append(currBuck.get(j).getCpscNum(), currBuck.get(j).getLoc());
					} else {
						buck2.append(currBuck.get(j).getCpscNum(), currBuck.get(j).getLoc());
					}
				}
			}
			
			// write the newly created buckets replacing the first one and appending the other
			writeIndex(stream, buck1, i * 400);
			writeIndex(stream, buck2, (numOfBuckets + i) * 400);
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method writeIndex (Prog2B)
    |
    | 		 Purpose:  Writes the 50 records of the bucket given to the correct offset in
    | 					the index file also given.
    |
    |  Pre-condition:  Make sure to pass the correct offset, which should be
    | 					the (length of 50 * 2 ints (4) bytes) * (the index of the bucket)
    |
    | Post-condition:  -
    |
    |     Parameters:  1. RandomAccessFile stream -- the stream to be written to
    |				   2. Bucket bucket -- the bucket to be written to the index file
    |				   3. int offset -- the offset of the bucket to be written
    |      	
    |        Returns:  -
    |
    *-------------------------------------------------------------------*/
	private static void writeIndex(RandomAccessFile stream, Bucket bucket, int offset) {
		
		try {
			stream.seek(offset);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Goes through the bucket and write all the fields to the file
		for(int i = 0; i < bucket.getBucket().length; i++) {
				
			try {
				stream.writeInt(bucket.get(i).getCpscNum());
				stream.writeInt(bucket.get(i).getLoc());
					
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method readIndex (Prog2B)
    |
    | 		 Purpose:  Reads the 50 records of the bucket given the correct offset in
    | 					the index file also given.
    |
    |  Pre-condition:  Make sure to pass the correct offset, which should be
    | 					the (length of 50 * 2 ints (4) bytes) * (the index of the bucket)
    |
    | Post-condition:  Use the bucket that is returned.
    |
    |     Parameters:  1. RandomAccessFile stream -- the stream to be read to
    |				   2. int offset -- the offset of the bucket to be read
    |      	
    |        Returns:  The bucket that was read
    |
    *-------------------------------------------------------------------*/
	public static Bucket readIndex(RandomAccessFile stream, int offset) {
		
		Bucket bucket = new Bucket(); // Bucket to be read
		
		try {
			stream.seek(offset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Goes through the file and read all the fields to the new Bucket
		for(int i = 0; i < bucket.getBucket().length; i++) {
			
			try {
				bucket.get(i).setCpscNum(stream.readInt());
				bucket.get(i).setLoc(stream.readInt());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return bucket;
	}
}

/*------------------------------------------------------------------
|  Class Name:  Bucket
|  	   Author:  Victor Gomes
|  
|	  Purpose:  Used to store 50 fields that have the CPSC # and a loc, forming
|				 one bucket. Which it will be stored in the index file.
|
|	  Methods:  A couple useful methods used to access and modify the bucket.
|
|						1. void append(int cpsc, int loc)
|						2. boolean isFilled()
|						3. int actualLength()
|						4. Field get(int index)
|						5. Field[] getBucket()
|						6. void printBucket()
|
*-------------------------------------------------------------------*/

class Bucket {
	
	/*-------------------------------------------------------------------
	 *   Class Name:  Field
	 *       Author:  Victor Gomes
	 * 
	 * 	Description:  A small inside class that represents a Field in the bucket
	 * 				   out of the 50 fields. It holds a CPSC # and a Location.
	 * 
	 *-------------------------------------------------------------------*/
	public class Field {
		private int cpscNum;
		private int loc;
		
		public Field(int cpsc, int loc) {
			this.setCpscNum(cpsc);
			this.setLoc(loc);
		}

		// Getters
		public int getCpscNum() { return cpscNum; }
		public void setCpscNum(int cpscNum) { this.cpscNum = cpscNum; }

		// Setters
		public int getLoc() { return loc; }
		public void setLoc(int loc) { this.loc = loc; }
	}
	
	private Field[] bucket; // Stores the bucket
	
	public Bucket() {
		
		// Initializes the 50 Fields in the bucket with the place holder of -1
		bucket = new Field[50];
		for(int i = 0; i < bucket.length; i++) {
			bucket[i] = new Field(-1, -1);
		}
	}

	/*-------------------------------------------------------------------
    |  Method append (Bucket)
    |
    | 		 Purpose:  To append to the last available place in the bucket
    |
    |  Pre-condition:  -
    |
    | Post-condition:  -
    |
    |     Parameters:  1. int cpsc -- the CPSC number
    | 				   2. int loc -- the location number
    |      	
    |        Returns:  -
    |
    *-------------------------------------------------------------------*/
	public void append(int cpsc, int loc) {

		// Go through array until a null or place holder element is found and append
		for(int i = 0; i < bucket.length; i++) {
			if(bucket[i] == null || bucket[i].getCpscNum() == -1) {
				bucket[i] = new Field(cpsc, loc);
				break;
			} else {
				continue;
			}
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method isFilled (Bucket)
    |
    | 		 Purpose:  To check if the bucket is full
    |
    |  Pre-condition:  -
    |
    | Post-condition:  -
    |
    |     Parameters:  -
    |      	
    |        Returns:  true if the bucket is filled, false otherwise.
    |
    *-------------------------------------------------------------------*/
	public boolean isFilled() {
		
		// return if the bucket has no more space
		if(bucket[49] == null || bucket[49].getCpscNum() == -1) {
			return false;
		} else {
			return true;
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method actualLength (Bucket)
    |
    | 		 Purpose:  Counts the place holders (-1) as the end of the bucket length,
    |					so we can have the length of all values that are not place holders.
    |
    |  Pre-condition:  -
    |
    | Post-condition:  -
    |
    |     Parameters:  -
    |      	
    |        Returns:  the length without counting place holders.
    |
    *-------------------------------------------------------------------*/
	public int actualLength() {
		
		// goes through the bucket elements, breaks when a place holder is found
		int result = 0;
		for(int i = 0; i < bucket.length; i++) {
			if(bucket[i].getCpscNum() == -1) {
				break;
			} else {
				result++;
			}
		}
		
		return result;
	}
	
	// Getters
	public Field get(int index) { return bucket[index]; }
	public Field[] getBucket() { return this.bucket; }
	
	/*-------------------------------------------------------------------
    |  Method printBucket (Bucket)
    |
    | 		 Purpose:  (For debugging purposes) It iterates through the bucket 
    |					and prints out the elements in it in a easy to see way.
    |
    |  Pre-condition:  -
    |
    | Post-condition:  -
    |
    |     Parameters:  -
    |      	
    |        Returns:  -
    |
    *-------------------------------------------------------------------*/
	public void printBucket() {
		
		// Goes through the field of the bucket and prints all values, 5 per line
		for(int i = 0; i < bucket.length; i++) {
			
			if((i + 1) % 5 == 0) {
				System.out.println();
			}
			System.out.print(i + ": " + bucket[i].getCpscNum() + " " + bucket[i].getLoc() + " || ");
		}
		
		System.out.println();
		System.out.println("---------");
		System.out.println();
	}
}
