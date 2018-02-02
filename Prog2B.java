import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

/*=============================================================================
|   Assignment:  Program #2 (Prog2B.java)
|       Author:  Victor Gomes (victorluizgomes@email.arizona.edu)
|
|       Course:  CSc 460 Spring 2018
|   Instructor:  L. McCann
|     Due Date:  January 18th, at the beginning of class
|
|     Language:  Java 1.8
|  Compile/Run:  When running this program, supplement it with an .tsv (tab-separated values)
|				  file as an argument, which will be used to convert it to a .bin file.
|
+-----------------------------------------------------------------------------
|
|  Description:  The program creates a binary version of a tab-separated values file that
|				  comes from a sample of hospital emergency room visits in 2016. The file works 
| 				  by first sorting the lines by the second field (trmt_date) in ascending order
|				  and then making sure all Strings are padded to the size of the biggest String,
|				  so that RandomAccessFile can successfully convert it to a Binary file. 
|                
|        Input:  The user will need to provide a tab-separated file as an argument.
|
|       Output:  The output is a newly created Binary file conversion of the .tsv file provided.
|
|   Techniques:  I used a TreeMap<Date, ArrayList<String>> as the way to store the data, 
| 				  it was done so that way because a TreeMap sorts automatically based on the 
|				  key (Date) in a fast manner. But it does not allow for duplicate values 
| 				  associated to that key, so that is where the ArrayList comes handy, being
| 				  able to add duplicates (same date lines) to the same key value.
|
|   Known Bugs:  None
|
*===========================================================================*/

// TODO Fix documentation

public class Prog2B {
	public static void main(String[] args) {
		
		AllDataRecords records; // to store the records 
		
		RandomAccessFile dataStream = null; // the strean to be read
		RandomAccessFile indexStream = null;
		File fileRef = null;				// to get the file from the arguments
		File idxFile = null;
		
		int h = 0; 				// The number of rehashings for the hash function
		
		int strRecordsLen = 0;  // the length in bytes of all the string fields
		int totalLineLen = 0;   // the whole line length TODO: not used
		long numOfRecords = 0;  // the number of records in the file
		
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
			
			Bucket bucket0 = new Bucket();
			Bucket bucket1 = new Bucket();
			int index = -1;
			
			dataStream.seek(0);
			for(int i = 1; i <= numOfRecords; i++) {
				records.readBinary(dataStream); // reads all fields of a line
				
				// Gets in which bucket we should put the Cpsc #
				index = hash(records.getCpscCase(), h);  
				
				if (i == 1) { // first time we just write to idx
					if(index == 0) {
						bucket0.append(records.getCpscCase(), totalLineLen * (i - 1));
					} else {
						bucket1.append(records.getCpscCase(), totalLineLen * (i - 1));
					}
					
					writeIndex(indexStream, bucket0, 0);
					writeIndex(indexStream, bucket1, 100 * 4); // 50 * 2 ints
				} else {
					// TODO: read obj from idx file
					
				}
			}
			
			// create new binary file named simplelinear.idx
			// which will be the index file (constructed with the CPSC # I have)
			
			// Create bucket objects?
			// if we only have two
			
			indexStream.close();
			dataStream.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// hash function returns the index for the cpsc number
	// hash(number) = number % Math.pow(number, H + 1)
	private static int hash(int number, int h) {
		
		return number % (int)Math.pow(number, h + 1);
	}
	
	// Writes all 50 ints as a bucket to the index
	private static void writeIndex(RandomAccessFile stream, Bucket bucket, int offset) {
		// TODO: offset should be the (length of 50 * 2 ints bytes) * (the index of the bucket)
		try {
			stream.seek(offset);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(int i = 0; i < bucket.getBucket().length; i++) {
			
			try {
				
				if(bucket.get(i) == null ) {
					stream.writeInt(-1);
					stream.writeInt(-1);
				} else {
					stream.writeInt(bucket.get(i).getCpscNum());
					stream.writeInt(bucket.get(i).getLoc());
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/*------------------------------------------------------------------
|  Class Name:  Bucket
|  	   Author:  Victor Gomes
|  
|	  Purpose:  TODO: fix. To store all the data of all the fields given on the sample file, 
|				 hold the biggest length of all the fields with String on it, have
|				 multiple getters and setters for all the instance variables.
|				 In addition, have the ability to write the information of those fields
|				 to a binary file when a RandomAccessFile is given. 
|
|	  Methods:  There are two instance methods that are used to write to a binary file.
|
|						1. void writeBinary(RandomAccessFile stream)
|						2. void writeMaxLengths(RandomAccessFile stream)
|
*-------------------------------------------------------------------*/

class Bucket {
	
	// Creates a Object field that holds the CPSC number and Location
	public class Field {
		private int cpscNum;
		private int loc;
		
		public Field(int cpsc, int loc) {
			this.setCpscNum(cpsc);
			this.setLoc(loc);
		}

		public int getCpscNum() { return cpscNum; }
		public void setCpscNum(int cpscNum) { this.cpscNum = cpscNum; }

		public int getLoc() { return loc; }
		public void setLoc(int loc) { this.loc = loc; }
	}
	
	private Field[] bucket;
	
	public Bucket() {
		
		bucket = new Field[50];
		for(int i = 0; i < bucket.length; i++) {
			bucket[i] = null;
		}
	}

	// Go through array until a null element is found and append
	public void append(int cpsc, int loc) {

		for(int i = 0; i < bucket.length; i++) {
			if(bucket[i] == null || bucket[i].getCpscNum() == -1) {
				bucket[i] = new Field(cpsc, loc);
				break;
			} else {
				continue;
			}
		}
	}
	
	public void put(int cpsc, int loc, int index) {
		bucket[index] = new Field(cpsc, loc);
	}
	
	public Field get(int index) {
		return bucket[index];
	}
	
	public Field[] getBucket() {
		return this.bucket;
	}
}
