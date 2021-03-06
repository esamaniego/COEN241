//import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.UnknownHostException;
import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
//import java.security.MessageDigest;

public class Server {
	
	static int numPartitions;
    static double partitionSize; 
    static String[][] partitionMapping;
    //static String[] diskMapping;
    static ArrayList<String> diskList;
    static ArrayList<String> portList;
    static ArrayList<String> fileList;
	
	public static void main(String[] args) throws IOException {
		  
		if (args.length < 5) {
			System.out.println("There should be at least five arguments: a partition power and at least two disk drives with their corresponding ports." );
			System.out.println("Sample usage: java Server 4 129.210.16.87 42598 129.210.16.88 33443");
			System.exit(1);
		}
		  
		int partitionPower = 0;
		try {
			partitionPower = Integer.parseInt(args[0]);
		} catch (NumberFormatException err){
			System.out.println("The first argument must be an integer.");
			System.exit(1);
		}
		  
		
		int port = findPort();
		System.out.println("Port number is: " + port);
		System.out.println("");
		  
		int bytesRead;  
	    ServerSocket serverSocket = new ServerSocket(port);
	    
	    long fileSize = 0;
	    numPartitions = (int)Math.pow(2,partitionPower);
	    partitionSize =	(Math.pow(2,32)) / numPartitions;
	    
	    
	    int diskCount = args.length - 1;
	    diskList = new ArrayList<String>();
	    portList = new ArrayList<String>();
	    fileList = new ArrayList<String>();
	    

	    for (int i=0; i < diskCount; i=i+2){
	    	diskList.add(args[i+1]);
	    	portList.add(args[i+2]);
	    }
	    
	    partitionMapping = new String[numPartitions][5];
	    mapPartitionToDrives();
	    	        	    
	    
	    while(true) {
	    	Socket clientSocket = null;
	        clientSocket = serverSocket.accept();
	         
	        InputStream in = clientSocket.getInputStream();
	         
	        DataInputStream clientData = new DataInputStream(in); 
	         
	        String command = (clientData.readUTF()).trim(); 
	        	        
	        
	        String commandArg = (command.substring(command.indexOf(' ') + 1)).trim();	        
	        command = (command.substring(0, command.indexOf(' '))).trim().toLowerCase();
	        String username = "";
        	String filename = "";
        	String newfilename = "";
	        
	        
	        if (command.startsWith("upload") || command.startsWith("download") || command.startsWith("delete")){
	        	username = (commandArg.substring(0,commandArg.indexOf('/'))).trim();
	        	filename = (commandArg.substring(commandArg.indexOf('/') + 1)).trim();
	        	newfilename = "_".concat(username).concat("_").concat(filename);
	        }

	        if (command.startsWith("upload")){
		        //OutputStream output = new FileOutputStream(fileName);  
		        OutputStream output = new FileOutputStream("/tmp/" + newfilename);
		        long size = clientData.readLong();
		        fileSize = size;
		        byte[] buffer = new byte[1024];   
		        while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1)   
		        {   
		            output.write(buffer, 0, bytesRead);   
		            size -= bytesRead;   
		        }
		        output.close();
	        }
	         
	       
	        
	        //
	        //implement command actions here
	        //
	        if (command.startsWith("download")){
	        	DownloadCommand(newfilename);
	        } else if (command.startsWith("list")){
	        	ListCommand();
	        } else if (command.startsWith("upload")){
	        	UploadCommand(newfilename,fileSize);
	        } else if (command.startsWith("delete")){
	        	try {
					DeleteCommand(newfilename);
				} catch (Exception e) {
					System.out.println("Exception: " + e);
					
				}
	        } else if (command.startsWith("add")){
	        	try {
					AddCommand(commandArg);
				} catch (Exception e) {
					System.out.println("Exception generated: " + e);
					System.out.println("Since file could not be moved to the new drive, no changes have been made. The new drive is not added. Please try again later");
					
				}
	        } else if (command.startsWith("remove")){
	        	RemoveCommand(commandArg);
	        } else {
	        	MyView();
	        	System.out.println("");
	        }
       
	        clientData.close();
	        in.close();
	    }
	}
	

	public static int findPort(){
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {
				
			}
				return port;
			} catch (IOException e) { 
				
			} finally {
				if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					
				}
			}
		}
		
		throw new IllegalStateException("Unable to find port");
	}

	
	static void DeleteCommand(String filename)throws Exception{
		String partitionValue;
		
		for (int i=0; i < numPartitions; i ++){
			partitionValue = partitionMapping[i][0];
			if (partitionValue != null && partitionValue.equals(filename)){
				partitionMapping[i][0] = null;
				partitionMapping[i][2] = null;
				partitionMapping[i][3] = null;
				
				Socket toDriveSock;
				toDriveSock = new Socket(partitionMapping[i][1], Integer.parseInt(partitionMapping[i][4]));
				OutputStream toDriveOs = toDriveSock.getOutputStream();
				DataOutputStream toDriveDos = new DataOutputStream(toDriveOs);
				toDriveDos.writeUTF("delete " + filename);
				
				
				System.out.println("File " + filename + " has been deleted");
			}
		}
		
		boolean containsItem = fileList.contains(filename);
		if (containsItem){
			fileList.remove(filename);
		} else {
			System.out.println("Unable to delete. File " + filename + " not found");
		}
			
		System.out.println("");
	}
	
	
	static void ListCommand(){
		System.out.println("File list:");
		for (String str : fileList) {
			System.out.println("  " + str);
		}
		System.out.println("");
	}
	
	
	static void UploadCommand(String filename, long fsize){
		long fileHashNum = FilenameHash(filename);	
		int neededPartition = (int)(Math.ceil(fsize/partitionSize));
		String checkSumString="";
		
		try {
			checkSumString = GetCheckSum("/tmp/" + filename);
		} catch (Exception e) {
			System.out.println("Exception generated: " + e);
			
		}
		
		boolean containsItem = fileList.contains(filename);
		if (!containsItem){
			fileList.add(filename);
			SaveToPartitionMappingTable(filename, fileHashNum, neededPartition, checkSumString);
		} else {
			UpdatePartionMappingTable(filename, fileHashNum, neededPartition, checkSumString);
		}
		
		DisplayObject(filename);		
	}
	
	
	static void DownloadCommand(String filename){
		//DisplayObject(filename);
		String currentFileName = "";
		String expectedCheckSum = "";
		String copiedFileCheckSum = "";
		String badFilePort = "";
		String badFileDrive = "";
		for (int i=0; i < numPartitions; i ++){
			currentFileName = partitionMapping[i][0];
			if (currentFileName != null && currentFileName.equals(filename)){
				try {
					String replicaNum = partitionMapping[i][2].substring(partitionMapping[i][2].length() - 1);
					CopyFromDisk(filename, partitionMapping[i][1], Integer.parseInt(partitionMapping[i][4]), replicaNum);
					expectedCheckSum = partitionMapping[i][3];
					//System.out.println("expected checksum: " + expectedCheckSum);
					try {
						copiedFileCheckSum = GetCheckSum("/tmp/" + filename + "_tmp");
						//System.out.println("copiedd  checksum: " + copiedFileCheckSum);
					} catch (Exception e) {
						System.out.println("Exception generated: " + e);
						
					}
					
					if (copiedFileCheckSum.equals(expectedCheckSum)){
						//System.out.println("File is good: " + partitionMapping[i][1]);
						File oldName = new File("/tmp/" + filename + "_tmp");
					    File newName = new File("/tmp/" + filename);
					    if (newName.exists()) {
					    	newName.delete();
					    }
					    oldName.renameTo(newName);
					} else {
						badFileDrive = partitionMapping[i][1];
						badFilePort = partitionMapping[i][4];
						//System.out.println("bad drive: " + badFileDrive);
						//System.out.println("badport: " + badFilePort);
					}
					
					
					
				} catch (NumberFormatException e) {
					System.out.println("NumberFormatException " + e);
					
				} catch (Exception e) {
					System.out.println("Exception " + e);
					
				}
			}
		}//end for loop
		

		//If corrupted file, update it
		if (!badFileDrive.equals("")){
			try {
				CopyToDisk(filename, badFileDrive, Integer.parseInt(badFilePort));
			} catch (Exception e) {
				System.out.println("Exception " + e);
				
			}
		}
				
		//display file		
		boolean containsItem = fileList.contains(filename);
		if (containsItem){
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader("/tmp/" + filename));
			} catch (FileNotFoundException e1) {
				System.out.println("FileNotFoundException generated: " + e1);
				
			}
			String line = null;
			try {
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				System.out.println("IOException generated: " + e);
				
			}

		} else {
			System.out.println("Unable to download. File " + filename + " not found");
		}
			
		System.out.println("");
		
	}
	
	
	static void AddCommand(String disk)throws Exception{
		String assignedDrive;
		int currentDiskCount = diskList.size();
		int newNumPartitionForEachDrive = (int) numPartitions/(currentDiskCount + 1);  //plus one since adding 1 extra disk
		int currentNumParttionForEachDrive = (int) numPartitions/currentDiskCount;
		int numOfPartitionsToGiveUp = currentNumParttionForEachDrive - newNumPartitionForEachDrive;
		int countGivenUp = 0;
		
		System.out.println("Processing add disk...");
		
    	String diskPort = (disk.substring(disk.indexOf('/') + 1)).trim();
    	disk = (disk.substring(0,disk.indexOf('/'))).trim();
    	
    	
    	//test connection to new drive before continuing to make changes
    	Socket toDriveSock;
		toDriveSock = new Socket(disk, Integer.parseInt(diskPort));
		OutputStream toDriveOs = toDriveSock.getOutputStream();
		DataOutputStream toDriveDos = new DataOutputStream(toDriveOs);
		toDriveDos.writeUTF("test connection ");
		
		toDriveDos.flush();
		toDriveDos.close();
		toDriveOs.close();
		toDriveSock.close();	
    	//end test connection to new drive
		
    	
		
		
		for (int j=0; j < currentDiskCount; j++){
			for (int i=0; i < numPartitions; i ++){
				assignedDrive = partitionMapping[i][1];
				if (assignedDrive.equals(diskList.get(j))){
					
					//case where partition is not empty
					
					if (partitionMapping[i][0] != null){
						String currentCheckSum = partitionMapping[i][3];
						String currentReplicaDesc = partitionMapping[i][2];
						
						String replicaDisk = FindReplicaDisk(currentCheckSum, currentReplicaDesc);
						System.out.println("The other copy is in disk " + replicaDisk);
						
						if (!(replicaDisk.equals(disk))){  //avoid copying both replica to the new disk
							//Move from old disk to new disk
							String filename = partitionMapping[i][0];
							try {
								CopyFromDisk(filename, partitionMapping[i][1], Integer.parseInt(partitionMapping[i][4]), partitionMapping[i][2]);
							} catch (NumberFormatException e) {
								System.out.println("NumberFormatException generated: " + e);
								
							} catch (Exception e) {
								System.out.println("Exception generated: " + e);
								
							}
			
							File oldName = new File("/tmp/" + filename + "_tmp");  //file that is being move to the new drive
							File newName = new File("/tmp/" + filename);
							if (newName.exists()) {
							    newName.delete();
							}
							oldName.renameTo(newName);
							
							try {
								CopyToDisk(filename, disk, Integer.parseInt(diskPort));
							} catch (Exception e) {
								System.out.println("Exception generated: " + e);
								
							}
							
							//the table will be updated if the file is moved
							partitionMapping[i][1] = disk;
							partitionMapping[i][4] = diskPort;
							countGivenUp++;
							
						} //end moving file to new drive
						
					} else { 	//end case where partition is not empty. if partition is not empty, just update the map table		
						partitionMapping[i][1] = disk;
						partitionMapping[i][4] = diskPort;
						countGivenUp++;

					}
				}
				if (countGivenUp == numOfPartitionsToGiveUp){
					countGivenUp = 0;
					break;
				}
			}
		}
		
		
		diskList.add(disk);
		portList.add(diskPort);
		
		System.out.println("");
		System.out.println("New partition table:");
		System.out.println("============================================");
		for (int i=0; i < numPartitions; i ++){
			System.out.println("partion: " + i + " " + partitionMapping[i][1] + "   "  + partitionMapping[i][0] );
		}
		System.out.println("");
	}
	
	
	static void RemoveCommand(String disk){
		System.out.println("Processing remove disk...");
		String assignedDrive;
		String filename;
		int index = 0;
		//diskList.remove(disk);  //remove the disk from the list
		//String diskPort = (disk.substring(disk.indexOf('/') + 1)).trim();
    	disk = (disk.substring(0,disk.indexOf('/'))).trim();
		
		
		int arraylistIndex = diskList.indexOf(disk);
		diskList.remove(arraylistIndex);
		portList.remove(arraylistIndex);
				
		
		for (int i=0; i < numPartitions; i ++){  //if equals to the disk to be removed, update the disk assignment by alternating between the remaining disk on the list.
			assignedDrive = partitionMapping[i][1];
			
			if (assignedDrive.equals(disk)){  //affected partition. Process it
				
				if(partitionMapping[i][0] != null){  //partition is not empty. need to move the file.
					String currentCheckSum = partitionMapping[i][3];
					String currentReplicaDesc = partitionMapping[i][2];
					
					String replicaDisk = FindReplicaDisk(currentCheckSum, currentReplicaDesc);
					System.out.println("The other copy is in disk " + replicaDisk);
					
					if (replicaDisk.equals(diskList.get(index))){
						System.out.println("replica is in the same disk, incrementing");
						if (index == (diskList.size()-1)){
							index = 0;
						} else {
							index++;
						}
					}
					
					//Move from old disk to new disk
					filename = partitionMapping[i][0];
					try {
						CopyFromDisk(filename, partitionMapping[i][1], Integer.parseInt(partitionMapping[i][4]), partitionMapping[i][2]);
					} catch (NumberFormatException e) {
						System.out.println("NumberFormatException generated: " + e);
						
					} catch (Exception e) {
						System.out.println("Exception generated: " + e);
						
					}
	
					File oldName = new File("/tmp/" + filename + "_tmp");  //file from the soon to be removed disk
					File newName = new File("/tmp/" + filename);
					if (newName.exists()) {
					    newName.delete();
					}
					oldName.renameTo(newName);
					
					try {
						CopyToDisk(filename, diskList.get(index), Integer.parseInt(portList.get(index)));
					} catch (Exception e) {
						System.out.println("Exception generated: " + e);
						
					}
				}
				//end of start of non-empty partition
						
				
				//Update mapping table
				partitionMapping[i][1] = diskList.get(index); 
				partitionMapping[i][4] = portList.get(index);

				
				if (index == (diskList.size()-1)){
					index = 0;
				} else {
					index++;
				}			
			}
		}
		
		System.out.println("");
		System.out.println("New partition table:");
		System.out.println("============================================");
		for (int i=0; i < numPartitions; i ++){
			System.out.println("partion: " + i + " " + partitionMapping[i][1] + "   "  + partitionMapping[i][0] );
		}
		System.out.println("");
	}
	
	
	static long FilenameHash(String filename){
		
		////////////////////////////////
		// divide string into chunks
		////////////////////////////////
	
		int strLen = filename.length();
		int chunkCount = (int)(Math.ceil(strLen / (double)4));
		String[] arrayOfChunks = new String[chunkCount];
		long[] arrayOfLongChunks = new long[chunkCount];
		
		for(int i = 0, x=0, y=4; i<chunkCount; i++){
			arrayOfChunks[i]  = filename.substring(x,y);
	        
	        //go to the next 4 char set
	        x += 4;
	        y = ( y + 4 ) <= strLen ? y + 4 : strLen;	//handles boundary
	    }
				
		
		////////////////////////////////
		// conversions, conversions, and conversions
		////////////////////////////////		
		
		for (int i = 0; i < chunkCount; i++){
	    	String hexEquivalent = asciiToHex(arrayOfChunks[i]);
	    	String bin = hexToBinary(hexEquivalent);

	    	////////////////////////////////
	    	// reverse bits of certain chunks
	    	////////////////////////////////
	    	if (i % 2 == 0){
	    		bin = new StringBuffer(bin).reverse().toString();    		
	    	}
	    	
	    	//newHex[i] = binaryToHex(bin);
	    	arrayOfLongChunks[i] = Long.parseLong(bin, 2);
	    }
		
		
		
		long xor_result = arrayOfLongChunks[0];  //put the first element to first operand
	    for (int i = 1; i < chunkCount; i++){
	    	xor_result = xor_result ^ arrayOfLongChunks[i];
	    }    
	    return xor_result;	
	}
	
	
	static String asciiToHex(String asciiValue){
		
		char[] chars = asciiValue.toCharArray();
	    StringBuffer hex = new StringBuffer();
	    for (int i = 0; i < chars.length; i++){
	    	hex.append(Integer.toHexString((int) chars[i]));
	    }
	      
	    //pad extra 0s if necessary
	    if (hex.length() < 8){
	    	int padding = 8 - hex.length();
	    	for (int i = 0; i < padding; i++){
	    		hex.append(0);
	    	}
	    }
	    return hex.toString();
	}
	
	
	static String hexToBinary(String s) {
		int num = (Integer.parseInt(s, 16));

		StringBuffer sb =  new StringBuffer(Integer.toBinaryString(num));
		while (sb.length() < 32){
			sb.insert(0, "0");  //add leading 0s as necessary
		}
		
		String str = sb.toString();
		return str;
	}
	
	
	static String binaryToHex(String bin){
		return Long.toHexString(Long.parseLong(bin,2));
	}
	
	
	static void mapPartitionToDrives(){
		int partionMemCounter = 0;
		int diskIndex = 0;
		int numPartionForEachDrive = (int) numPartitions/diskList.size();
		
		for (int i=0; i < numPartitions; i++){
			partitionMapping[i][1] = diskList.get(diskIndex);
			partitionMapping[i][4] = portList.get(diskIndex);

			partionMemCounter++;

			if (partionMemCounter == numPartionForEachDrive){
				partionMemCounter = 0;
				if (i < ((numPartionForEachDrive*diskList.size())-1)){  //move to the next disk on the list. Partitions not evenly divided (remaining partitions) will be assigned to last disk
					diskIndex ++;
				}
			}
		}
					    
	}
	
	
	static void SaveToPartitionMappingTable (String filename, long hash, int neededPartition, String checkSumString){
		int zeroBasedNumPartition = numPartitions - 1;
		long temp =  hash % zeroBasedNumPartition;
		int partitionIndex = (int)temp;
		
		for (int i = 0; i < neededPartition; i++){
			String partitionValue = partitionMapping[partitionIndex][0];
			
			while (partitionValue != null){
				partitionIndex++;
				partitionValue = partitionMapping[partitionIndex][0];

				
				if (partitionIndex >= zeroBasedNumPartition){
					partitionIndex = partitionIndex % zeroBasedNumPartition;
					partitionValue = partitionMapping[partitionIndex][0];
				}
					
			}
			partitionMapping[partitionIndex][0]=filename;	//column 0 gets the filename
			partitionMapping[partitionIndex][2]="replicate 1";  //column 2 gets either replicate 1 or replicate 2
			partitionMapping[partitionIndex][3]=checkSumString;  //column 3 get the checksum
			
			
			
//			boolean containsItem = fileList.contains(filename);
//			if (!containsItem){
//				fileList.add(filename);
//			}
			
			String diskSaved = partitionMapping[partitionIndex][1];  
			int portSaved = Integer.parseInt(partitionMapping[partitionIndex][4]);
			
			try {
				CopyToDisk(filename, diskSaved, portSaved);
			} catch (Exception e) {
				System.out.println("Exception generated: " + e);
				
			}
			
			
			ReplicateToPartitionMappingTable(filename, diskSaved, partitionIndex, checkSumString);
			

			partitionIndex++;
			if (partitionIndex > zeroBasedNumPartition){
				partitionIndex = partitionIndex % zeroBasedNumPartition;
			}
		}
		
	}
	
	
	static void ReplicateToPartitionMappingTable (String filename, String diskSaved, int partitionIndex, String checkSumString){
		
	
		int zeroBasedNumPartition = numPartitions - 1;
		int replicatePartitionIndex = partitionIndex + (numPartitions/2); //replicate halfway
		if (replicatePartitionIndex >= zeroBasedNumPartition){
			replicatePartitionIndex = replicatePartitionIndex % zeroBasedNumPartition;
		}
		
		
		
		String partitionDiskAssignment = partitionMapping[replicatePartitionIndex][1];
		String partitionValue = partitionMapping[replicatePartitionIndex][0];
		
		while (partitionDiskAssignment.equals(diskSaved) || partitionValue != null ){
			replicatePartitionIndex++;
			partitionDiskAssignment = partitionMapping[replicatePartitionIndex][1];
			partitionValue = partitionMapping[replicatePartitionIndex][1];
			
			if (replicatePartitionIndex >= zeroBasedNumPartition){
				replicatePartitionIndex = replicatePartitionIndex % zeroBasedNumPartition;	
				partitionValue = partitionMapping[replicatePartitionIndex][0];
				partitionDiskAssignment = partitionMapping[replicatePartitionIndex][1];
			}
	
		}
		partitionMapping[replicatePartitionIndex][0]=filename;
		partitionMapping[replicatePartitionIndex][2]="replicate 2";
		partitionMapping[replicatePartitionIndex][3]=checkSumString;
		
		try {
			CopyToDisk(filename, partitionMapping[replicatePartitionIndex][1], Integer.parseInt(partitionMapping[replicatePartitionIndex][4]));
		} catch (Exception e) {
			System.out.println("Exception generated: " + e);
			
		}
		
	}
	
	static void UpdatePartionMappingTable(String filename, long hash, int neededPartition, String checkSumString){
		String currentFileName = "";
		for (int i=0; i < numPartitions; i ++){
			currentFileName = partitionMapping[i][0];
			if (currentFileName != null && currentFileName.equals(filename)){
				partitionMapping[i][3] = checkSumString;
				
				try {
					CopyToDisk(filename, partitionMapping[i][1], Integer.parseInt(partitionMapping[i][4]));
				} catch (Exception e) {
					System.out.println("Exception generated: " + e);
					
				}
			}
		}
		
	}
	
	static void DisplayObject(String filename){
		String partitionValue;

		boolean found = false;
		for (int i=0; i < numPartitions; i ++){
			partitionValue = partitionMapping[i][0];
			if (partitionValue != null && partitionValue.equals(filename)){
				System.out.println(partitionMapping[i][2] + ": partition " + i + " at disk " + partitionMapping[i][1]);
				found = true;
			}
		}
		if (!found){
			System.out.println("File " + filename + " does not exists");
		}
		System.out.println("");		
	}
	
	static String GetCheckSum(String datafile) throws Exception{

		MessageDigest md = MessageDigest.getInstance("SHA1");
		FileInputStream fis = new FileInputStream(datafile);
		byte[] dataBytes = new byte[1024];
		 
		int nread = 0; 
		 
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		 
		byte[] mdbytes = md.digest();
		 
		//convert the byte to hex format
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		fis.close();
		return sb.toString();
	}

	static void CopyToDisk(String filename, String drive, int port) throws Exception{
		Socket toDriveSock = new Socket(drive, port);
		OutputStream toDriveOs = toDriveSock.getOutputStream();
		DataOutputStream toDriveDos = new DataOutputStream(toDriveOs);
		toDriveDos.writeUTF("upload " + filename);
		
		File fileToDrive = new File("/tmp/" + filename);
		byte[] toDriveByteArray = new byte[(int)fileToDrive.length()];
		FileInputStream toDriveFis = new FileInputStream(fileToDrive);
		BufferedInputStream toDriveBis = new BufferedInputStream(toDriveFis);
		DataInputStream toDriveDis = new DataInputStream(toDriveBis);
		toDriveDis.readFully(toDriveByteArray, 0, toDriveByteArray.length);
		toDriveDos.writeLong(toDriveByteArray.length);
		toDriveDos.write(toDriveByteArray, 0, toDriveByteArray.length);
		
		toDriveDos.flush();
		toDriveDis.close();
		toDriveDos.close();
		toDriveOs.close();
		toDriveSock.close();
		System.out.println("File " + filename + " uploaded to disk " + drive);
		System.out.println("");
		
	}

	static void CopyFromDisk(String filename, String drive, int port, String replica) throws Exception{
		Socket fromDriveSock = new Socket (drive, port);
		OutputStream fromDriveOs = fromDriveSock.getOutputStream();
		DataOutputStream fromDriveDos = new DataOutputStream(fromDriveOs);
		fromDriveDos.writeUTF("download " + filename);
		InputStream fromDriveIs = fromDriveSock.getInputStream();
		int fromDriveBytesRead;
		OutputStream fromDriveOutput = new FileOutputStream("/tmp/" + filename  + "_tmp");
		DataInputStream fromDriveClientData = new DataInputStream(fromDriveIs);
		long size = fromDriveClientData.readLong();
		byte[] fromDriveBuffer = new byte[1024];
		while (size > 0 && (fromDriveBytesRead = fromDriveClientData.read(fromDriveBuffer, 0, (int)Math.min(fromDriveBuffer.length, size))) != -1) {
			fromDriveOutput.write(fromDriveBuffer, 0, fromDriveBytesRead);
			size -= fromDriveBytesRead;
		}
		fromDriveOutput.close();
		System.out.println("File " + filename + " copied from disk " + drive);
		System.out.println("");
	}
	
	static String FindReplicaDisk (String checkSum, String replicaDesc){
		String disk = "";
		for (int i=0; i < numPartitions; i ++){
			if (partitionMapping[i][3] != null){
				if(partitionMapping[i][3].equals(checkSum) && !(partitionMapping[i][2].equals(replicaDesc)) ){
					disk = partitionMapping[i][1];
					break;
				}
			}
		}
		return disk;
	}

	static void MyView(){
		System.out.println("Secret command. For debugging purposes");
		for (String s : diskList)
			System.out.println(s);
		
		System.out.println("");
		
		for (String s : portList)
			System.out.println(s);
		
		System.out.println("");
		
		
		
		
		for (int i=0; i < numPartitions; i ++){
			System.out.println(partitionMapping[i][0] + "  " + partitionMapping[i][1] + "  " + partitionMapping[i][2] + "  " + partitionMapping[i][3]  + "  " + partitionMapping[i][4]);
		}
		
	}

}
