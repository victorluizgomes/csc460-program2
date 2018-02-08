import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

/*=============================================================================
|   Assignment:  Program #2 (Prog2C.java)
|       Author:  Victor Gomes (victorluizgomes@email.arizona.edu)
|
|       Course:  CSc 460 Spring 2018
|   Instructor:  L. McCann
|     Due Date:  February 8th, at the beginning of class
|
|     Language:  Java 1.8
|  Compile/Run:  When running this program, supplement it with an .idx (index)
|				  file as an argument and a .bin (binary). When prompted, provide a
|				  CPSC number to be searched in the database file.
|
+-----------------------------------------------------------------------------
|
|  Description:  It provides with the user a search on the database using the SLH index,
|				  which are both given as command line arguments. It will prompt the user to 
|				  input the key it wants to search in the database and will output some
|				  information about that key if the key is found. 
|                
|        Input:  The user will need to provide a index file and a binary file as an argument.
|				  In addition, when program is running user must provide a CPSC number, and if
|				  the user wants to end the program just provide any negative number.
|
|       Output:  The output is the cpsc number, trmt date and narr1 fields of the line in which
|				  the CPSC number provided by the user are.
|
|   Techniques:  -
|
|   Known Bugs:  None
|
*===========================================================================*/

public class Prog2C {
	public static void main(String[] args) {
		
		AllDataRecords records; 				// to store the records 
		
		RandomAccessFile dbStream = null; 		// the database stream to be read
		RandomAccessFile indexStream = null;	// the index stream to be read
		File dbFile = null;						// to get the db file from the arguments
		File idxFile = null;					// to get the index file from the arguments
		
		Bucket bucket = null;					// A bucket holder to read from index
		
		int seekLoc = 0;						// the location of where to seek in the index
		int h = 0;								// number of rehashs
		int hashNum = 0;						// result of running the hash function
		
		Scanner sc = new Scanner(System.in);    // To take input from the user
		
		try {
			// first command line argument is the index file
			idxFile = new File(args[0]);
			indexStream = new RandomAccessFile(idxFile, "rw");
			
			// read the h at the end of the file
			indexStream.seek(indexStream.length() - 4);
			h = indexStream.readInt();
			indexStream.seek(0);

			// second command line argument is the database file
			dbFile = new File(args[1]);
			dbStream = new RandomAccessFile(dbFile, "rw");
			records = new AllDataRecords();
			
			// Read the last 6 ints containing the lengths of the string fields (And number of records)
			dbStream.seek(dbStream.length() - 28);
			records.setDateLength(dbStream.readInt());
			records.setStratumLength(dbStream.readInt());
			records.setRaceOtherLength(dbStream.readInt());
			records.setDiagOtherLength(dbStream.readInt());
			records.setNarr1Length(dbStream.readInt());
			records.setNarr2Length(dbStream.readInt());
			records.setNumOfRecords(dbStream.readInt());
			
			// Loop thorugh user input until negative key number is given
			while(true) {
				
				System.out.print("Enter A CPSC Case #: ");
				int cpsc = sc.nextInt();
				
				// If the CPSC number is negative break out of loop
				if(cpsc < 0) break;
				
				// hash the number given and get the bucket of where it should be
				hashNum = Prog2B.hash(cpsc, h);
				bucket = Prog2B.readIndex(indexStream, hashNum * 400);
				
				// Searches the current bucket for the CPSC number
				seekLoc = -1;
				for(int i = 0; i < bucket.actualLength(); i++) {
					
					if(bucket.get(i).getCpscNum() == cpsc) {
						seekLoc = bucket.get(i).getLoc();
						break;
					}
				}
				
				// if the seekLoc was never set, the cpsc # was not found
				if(seekLoc == -1) {
					System.out.println("The key '" + cpsc + "' was not found.");
					continue;
				}
				
				// Print the cpscCase number, trmt date and narr1 associated with cpsc read
				dbStream.seek(seekLoc);
				records.readBinary(dbStream);
				System.out.println("Case #    trmt_date  narr1");
				System.out.print(records.getCpscCase() + " ");
				System.out.print(records.getTrmt_date() + " ");
				System.out.println(records.getNarr1());
			}
			
			sc.close();
			indexStream.close();
			dbStream.close();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
