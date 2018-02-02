import java.io.*;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

/*=============================================================================
|   Assignment:  Program #2 (Prog2A.java)
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

public class Prog2A {
	public static void main(String[] args) {
		Integer 		 cpsc = null;									  // stores the CPSC Case #
		String 			 currentLine = null;							  // stores current line of file
		String[] 		 strArr = null;
		String[] 		 splitted = null;								  // for Strings between \t
		String 			 binFileName = null;							  // the filename.bin
		
		// A Tree Map automatically orders by keys(Date), but does not accept duplicates
		// But with an ArrayList for value you can bypass that.
		TreeMap<Integer, ArrayList<String>> tmap = new TreeMap<Integer, ArrayList<String>>();
		
		AllDataRecords 	 records;			// holds all the fields
		RandomAccessFile dataStream = null; // specializes the file I/O
		File 			 fileRef = null;    // used to create the file
		FileReader 		 fileR = null;      // reading the character file
		BufferedReader 	 buffR = null;		// parsing through the file line by line
		
		try {
			fileR = new FileReader(args[0]);
			buffR = new BufferedReader(fileR);
			buffR.readLine(); // Ignores first line
			
			// Takes the file name without the .tsv, without the path behind it and concatenates .bin to it
			int lastIndex = args[0].lastIndexOf('/');
			if(lastIndex == -1)
				binFileName = args[0].substring(0, args[0].length() - 4) + ".bin";
			else
				binFileName = args[0].substring(lastIndex + 1, args[0].length() - 4) + ".bin";
			fileRef = new File(binFileName);
			dataStream = new RandomAccessFile(fileRef, "rw");
			records = new AllDataRecords();
			
			// Parses through every line of the file
			while((currentLine = buffR.readLine()) != null) {
				
				// Split the line in the tab to get all fields
				strArr = new String[19];
				splitted = currentLine.split("\t");
				
				// Get a array that always has 19 fields (even if split returns 18)
				for(int i = 0; i < strArr.length; i++) {
					
					if(i == 18 && splitted.length <= 18) {
						strArr[i] = "";
					} else {
						
						strArr[i] = splitted[i];
					}
				}
				
				// Sets the maximum lengths of the file
				setupMaxLength(strArr, records);
				
				// Get the date from the currentLine's trmt_date
				cpsc = Integer.parseInt(strArr[0]);
				
				// if there is not an ArrayList corresponding to the key, create one
				if(tmap.get(cpsc) == null) {
					
					ArrayList<String> arr = new ArrayList<String>();
					arr.add(currentLine);
					tmap.put(cpsc, arr);
				} else { // otherwise add to existing ArrayList
					
					ArrayList<String> arr = tmap.get(cpsc);
					arr.add(currentLine);
					tmap.put(cpsc, arr);
				}
			}
			
			int numberOfrec = 0; // store the number of records written
			
			// Prints all the ordered lines with an additional inside loop for the duplicate dates
			for(Entry<Integer, ArrayList<String>> entry : tmap.entrySet()) {
				for(int i = 0; i < entry.getValue().size(); i++) {
					
					// Get the current line and split it
					currentLine = entry.getValue().get(i);
					System.out.println(currentLine);
					strArr = new String[19];
					splitted = currentLine.split("\t");
					
					// Get a array that always has 19 fields (even if split returns 18)
					for(int j = 0; j < strArr.length; j++) {
						
						if(j == 18 && splitted.length <= 18) {
							strArr[j] = "";
						} else {
							
							strArr[j] = splitted[j];
						}
					}
					
					// Uses setters to setup the records
					setupRecords(strArr, records);
					
					numberOfrec++;
					
					// Write to the Binary File
					records.writeBinary(dataStream);
				}
			}
			
			records.setNumOfRecords(numberOfrec);
			
			// write at the end of the binary file the maximum lengths
			records.writeMaxLengths(dataStream);
			
			fileR.close();
			buffR.close();
			dataStream.close();
			
		// Error catching
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method setupMaxLength (Prog1A)
    |
    | 		 Purpose:  To be ran everytime a line is parsed through storing in the records
    |					the biggest lengths of the fields that contain strings.
    |
    |  Pre-condition:  The array given must be of the splitted fields correctly in place.
    |
    | Post-condition:  None
    |
    |     Parameters:  1. String[] arr -- splitted array of the fields
    |				   2. AllDataRecords rec -- holds the length which will be modified
    |      	
    |        Returns:  None
    |
    *-------------------------------------------------------------------*/
	private static void setupMaxLength(String[] arr, AllDataRecords rec) {
		
		if(rec.getDateLength() < arr[1].length())
			rec.setDateLength(arr[1].length());
		
		if(rec.getStratumLength() < arr[4].length()) 
			rec.setStratumLength(arr[4].length());
		
		if(rec.getRaceOtherLength() < arr[8].length()) 
			rec.setRaceOtherLength(arr[8].length());
		
		if(rec.getDiagOtherLength() < arr[10].length()) 
			rec.setDiagOtherLength(arr[10].length());
		
		if(rec.getNarr1Length() < arr[17].length()) 
			rec.setNarr1Length(arr[17].length());
		
		if(rec.getNarr2Length() < arr[18].length())
			rec.setNarr2Length(arr[18].length());
	}
	
	/*-------------------------------------------------------------------
    |  Method setupRecords (Prog1A)
    |
    | 		 Purpose:  To be ran everytime a line is parsed through storing in the different
    |					fields, so that they can be written to the Binary File.
    |
    |  Pre-condition:  The array given must be of the splitted fields correctly in place.
    |
    | Post-condition:  Use those fields to write them to the Binary File.
    |
    |     Parameters:  1. String[] arr -- splitted array of the fields
    |				   2. AllDataRecords rec -- holds the fields which will be set
    |      	
    |        Returns:  None
    |
    *-------------------------------------------------------------------*/
	private static void setupRecords(String[] arr, AllDataRecords rec) {
		
		if(!arr[0].equals("")) rec.setCpscCase(Integer.parseInt(arr[0]));
		else rec.setCpscCase(-1);
		rec.setTrmt_date(arr[1]);
		if(!arr[2].equals("")) rec.setPsu(Integer.parseInt(arr[2]));
		else rec.setPsu(-1);
		if(!arr[3].equals("")) rec.setWeight(Double.parseDouble(arr[3]));
		else rec.setWeight(-1);
		rec.setStratum(arr[4]);
		if(!arr[5].equals("")) rec.setAge(Integer.parseInt(arr[5]));
		else rec.setAge(-1);
		if(!arr[6].equals("")) rec.setSex(Integer.parseInt(arr[6]));
		else rec.setSex(-1);
		if(!arr[7].equals("")) rec.setRace(Integer.parseInt(arr[7]));
		else rec.setRace(-1);
		rec.setRace_other(arr[8]);
		if(!arr[9].equals("")) rec.setDiag(Integer.parseInt(arr[9]));
		else rec.setDiag(-1);
		rec.setDiag_other(arr[10]);
		if(!arr[11].equals("")) rec.setBody_part(Integer.parseInt(arr[11]));
		else rec.setBody_part(-1);
		if(!arr[12].equals("")) rec.setDisposition(Integer.parseInt(arr[12]));
		else rec.setDisposition(-1);
		if(!arr[13].equals("")) rec.setLocation(Integer.parseInt(arr[13]));
		else rec.setLocation(-1);
		if(!arr[14].equals("")) rec.setFmv(Integer.parseInt(arr[14]));
		else rec.setFmv(-1);
		if(!arr[15].equals("")) rec.setProd1(Integer.parseInt(arr[15]));
		else rec.setProd1(-1);
		if(!arr[16].equals("")) rec.setProd2(Integer.parseInt(arr[16]));
		else rec.setProd2(-1);
		rec.setNarr1(arr[17]);
		rec.setNarr2(arr[18]);
	}
}

/*------------------------------------------------------------------
|  Class Name:  AllDataRecords
|  	   Author:  Victor Gomes
|  
|	  Purpose:  To store all the data of all the fields given on the sample file, 
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

class AllDataRecords {

	// The maximum length allowed for Strings
	private int dateLength = 0;
	private int stratumLength = 0;
	private int raceOtherLength = 0;
	private int diagOtherLength = 0;
	private int narr1Length = 0;
	private int narr2Length = 0;
	private int numOfRecords = 0;
	
	// All data fields from the sample file provided (exact name representation)
	private    int cpscCase;
	private String trmt_date;
	private    int psu;
	private double weight;
	private String stratum;
	private    int age;
	private    int sex;
	private    int race;
	private String race_other;
	private    int diag;
	private String diag_other;
	private    int body_part;
	private    int disposition;
	private    int location;
	private    int fmv; 
	private    int prod1;
	private    int prod2;
	private String narr1;
	private String narr2;
	
	// Getters for the maximum length variables
	public int getDateLength() { return dateLength; }
	public int getStratumLength() { return stratumLength; }
	public int getRaceOtherLength() { return raceOtherLength; }
	public int getDiagOtherLength() { return diagOtherLength; }
	public int getNarr1Length() { return narr1Length; }
	public int getNarr2Length() { return narr2Length; }
	public int getnumOfRecords() { return numOfRecords; }
	
	// Setters for the maximum length variables
	public void setDateLength(int dateLength) { this.dateLength = dateLength; }
	public void setStratumLength(int stratumLength) { this.stratumLength = stratumLength; }
	public void setRaceOtherLength(int raceOtherLength) { this.raceOtherLength = raceOtherLength; }
	public void setDiagOtherLength(int diagOtherLength) { this.diagOtherLength = diagOtherLength; }
	public void setNarr1Length(int narr1Length) { this.narr1Length = narr1Length; }
	public void setNarr2Length(int narr2Length) { this.narr2Length = narr2Length; }
	public void setNumOfRecords(int numOfRecords) { this.numOfRecords = numOfRecords; }
	
	// Getters for the data fields
	public int getCpscCase() { return cpscCase; }
	public String getTrmt_date() { return trmt_date; }
	public int getPsu() { return psu; }
	public double getWeight() { return weight; }
	public String getStratum() { return stratum; }
	public int getAge() { return age; }
	public int getSex() { return sex; }
	public int getRace() { return race; }
	public String getRace_other() { return race_other; }
	public int getDiag() { return diag; }
	public String getDiag_other() { return diag_other; }
	public int getBody_part() { return body_part; }
	public int getDisposition() { return disposition; }
	public int getLocation() { return location; }
	public int getFmv() { return fmv; }
	public int getProd1() { return prod1; }
	public int getProd2() { return prod2; }
	public String getNarr1() { return narr1; }
	public String getNarr2() { return narr2; }
	
	// Setters for the data fields
	public void setCpscCase(int cpscCase) { this.cpscCase = cpscCase; }
	public void setTrmt_date(String trmt_date) { this.trmt_date = trmt_date; }
	public void setPsu(int psu) { this.psu = psu; }
	public void setWeight(double weight) { this.weight = weight; }
	public void setStratum(String stratum) { this.stratum = stratum; }
	public void setAge(int age) { this.age = age; }
	public void setSex(int sex) { this.sex = sex; }
	public void setRace(int race) { this.race = race; }
	public void setRace_other(String race_other) { this.race_other = race_other; }
	public void setDiag(int diag) { this.diag = diag; }
	public void setDiag_other(String diag_other) { this.diag_other = diag_other; }
	public void setBody_part(int body_part) { this.body_part = body_part; }
	public void setDisposition(int disposition) { this.disposition = disposition; }
	public void setLocation(int location) { this.location = location; }
	public void setFmv(int fmv) { this.fmv = fmv; }
	public void setProd1(int prod1) { this.prod1 = prod1; }
	public void setProd2(int prod2) { this.prod2 = prod2; }
	public void setNarr1(String narr1) { this.narr1 = narr1; }
	public void setNarr2(String narr2) { this.narr2 = narr2; }
	
	/*-------------------------------------------------------------------
    |  Method writeBinary (AllDataRecords)
    |
    | 		 Purpose:  Write every field of the current line to the Binary File given 
    |					and make sure to pad the strings all the same size, using the 
    | 					maximum length variables.
    |
    |  Pre-condition:  A correct RAF stream must be provided, so it can be written to.
    |
    | Post-condition:  None
    |
    |     Parameters:  1. RandomAccessFile stream -- the file stream to be written to
    |      	
    |        Returns:  None
    |
    *-------------------------------------------------------------------*/
	public void writeBinary(RandomAccessFile stream) {
		
		// Create paddable stringBuffers
		StringBuffer dateSB = new StringBuffer(trmt_date);
		StringBuffer stratumSB = new StringBuffer(stratum);
		StringBuffer raceOtherSB = new StringBuffer(race_other);
		StringBuffer diagOtherSB = new StringBuffer(diag_other);
		StringBuffer narr1SB = new StringBuffer(narr1);
		StringBuffer narr2SB = new StringBuffer(narr2);
		
		// Write to the Binary file
		try {
			stream.writeInt(cpscCase);
			dateSB.setLength(dateLength);
			stream.writeBytes(dateSB.toString());
			stream.writeInt(psu);
			stream.writeDouble(weight);
			stratumSB.setLength(stratumLength);
			stream.writeBytes(stratumSB.toString());
			stream.writeInt(age);
			stream.writeInt(sex);
			stream.writeInt(race);
			raceOtherSB.setLength(raceOtherLength);
			stream.writeBytes(raceOtherSB.toString());
			stream.writeInt(diag);
			diagOtherSB.setLength(diagOtherLength);
			stream.writeBytes(diagOtherSB.toString());
			stream.writeInt(body_part);
			stream.writeInt(disposition);
			stream.writeInt(location);
			stream.writeInt(fmv);
			stream.writeInt(prod1);
			stream.writeInt(prod2);
			narr1SB.setLength(narr1Length);
			stream.writeBytes(narr1SB.toString());
			narr2SB.setLength(narr2Length);
			stream.writeBytes(narr2SB.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*-------------------------------------------------------------------
    |  Method readBinary (AllDataRecords)
    |
    | 		 Purpose:  Reads all fields of the current line interpreting from a 
    |					given binary file.
    |
    |  Pre-condition:  A correct RAF stream must be provided, so it can be read.
    |
    | Post-condition:  None
    |
    |     Parameters:  1. RandomAccessFile stream -- the file stream to be read
    |      	
    |        Returns:  None
    |
    *-------------------------------------------------------------------*/
	public void readBinary(RandomAccessFile stream) {
		
		// Create a byte array for all the string fields with the max length
		byte[] dateByte = new byte[dateLength];
		byte[] stratumByte = new byte[stratumLength];
		byte[] raceOtherByte = new byte[raceOtherLength];
		byte[] diagOtherByte = new byte[diagOtherLength];
		byte[] narr1Byte = new byte[narr1Length];
		byte[] narr2Byte = new byte[narr2Length];
		
		// Read the fields from Binary file
		try {
			cpscCase = stream.readInt();
			stream.readFully(dateByte);
			trmt_date = new String(dateByte);
			psu = stream.readInt();
			weight = stream.readDouble();
			stream.readFully(stratumByte);
			stratum = new String(stratumByte);
			age = stream.readInt();
			sex = stream.readInt();
			race = stream.readInt();
			stream.readFully(raceOtherByte);
			race_other = new String(raceOtherByte);
			diag = stream.readInt();
			stream.readFully(diagOtherByte);
			diag_other = new String(diagOtherByte);
			body_part = stream.readInt();
			disposition = stream.readInt();
			location = stream.readInt();
			fmv = stream.readInt();
			prod1 = stream.readInt();
			prod2 = stream.readInt();
			stream.readFully(narr1Byte);
			narr1 = new String(narr1Byte);
			stream.readFully(narr2Byte);
			narr2 = new String(narr2Byte);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/*-------------------------------------------------------------------
    |  Method writeMaxLengths (AllDataRecords)
    |
    | 		 Purpose:  Write the String fields maximum lengths on the binary file, because
    |					the lengths of the text fields might be different for different 
    |					input files. (For when reading back from binary)
    |
    |  Pre-condition:  A correct RAF stream must be provided, so it can be written to.
    |
    | Post-condition:  None
    |
    |     Parameters:  1. RandomAccessFile stream -- the file stream to be written to
    |      	
    |        Returns:  None
    |
    *-------------------------------------------------------------------*/
	public void writeMaxLengths(RandomAccessFile stream) {
		
		try {
			stream.writeInt(dateLength);
			stream.writeInt(stratumLength);
			stream.writeInt(raceOtherLength);
			stream.writeInt(diagOtherLength);
			stream.writeInt(narr1Length);
			stream.writeInt(narr2Length);
			stream.writeInt(numOfRecords);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

