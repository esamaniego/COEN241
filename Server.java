import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	
	static int numPartitions;
    static double partitionSize; 
    static String[][] partitionMapping;
    //static String[] diskMapping;
    static ArrayList<String> diskList;
    static ArrayList<String> fileList;
	
	public static void main(String[] args) throws IOException {
		  
		if (args.length < 3) {
			System.out.println("There should be at least three arguments: a partition power and at least two disk drives." );
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
		  
		int bytesRead;  
	    ServerSocket serverSocket = new ServerSocket(port);
	    
	    long fileSize = 0;
	    numPartitions = (int)Math.pow(2,partitionPower);
	    partitionSize =	(Math.pow(2,32)) / numPartitions;
	    
	    
	    int diskCount = args.length - 1;
	    diskList = new ArrayList<String>();
	    fileList = new ArrayList<String>();
	    

	    for (int i=0; i < diskCount; i++){
	    	diskList.add(args[i+1]);
	    }
	    
	    partitionMapping = new String[numPartitions][3];
	    mapPartitionToDrives();
	    	        	    
	    
	    while(true) {
	    	Socket clientSocket = null;
	        clientSocket = serverSocket.accept();
	         
	        InputStream in = clientSocket.getInputStream();
	         
	        DataInputStream clientData = new DataInputStream(in); 
	         
	        String command = clientData.readUTF(); 
	        String commandArg = (command.substring(command.indexOf(' ') + 1)).trim();
	        command = (command.substring(0, command.indexOf(' '))).trim().toLowerCase();

	        if (command.startsWith("upload")){
		        //OutputStream output = new FileOutputStream(fileName);  
		        OutputStream output = new FileOutputStream("/tmp/a.txt");
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
	        	DownloadCommand(commandArg);
	        } else if (command.startsWith("list")){
	        	ListCommand();
	        } else if (command.startsWith("upload")){
	        	UploadCommand(commandArg,fileSize);
	        } else if (command.startsWith("delete")){
	        	DeleteCommand(commandArg);
	        } else if (command.startsWith("add")){
	        	AddCommand(commandArg);
	        } else if (command.startsWith("remove")){
	        	RemoveCommand(commandArg);
	        } else {
	        	System.out.println("Unknown command: " + command + ". Unable to process");
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
	
	static void DeleteCommand(String filename){
		String partitionValue;
		
		for (int i=0; i < numPartitions; i ++){
			partitionValue = partitionMapping[i][0];
			if (partitionValue != null && partitionValue.equals(filename)){
				partitionMapping[i][0] = null;
				System.out.println("File " + filename + " has been deleted");
			}
		}
		
		boolean containsItem = fileList.contains(filename);
		if (containsItem){
			fileList.remove(filename);
		} else {
			System.out.println("File " + filename + " not found");
		}
			
		System.out.println("");
	}
	
	
	static void ListCommand(){
		System.out.println("File list:");
		for (String str : fileList) {
			System.out.println("  " + str);
		}
	}
	
	
	static void UploadCommand(String filename, long fsize){
		long fileHashNum = FilenameHash(filename);	
		int neededPartition = (int)(Math.ceil(fsize/partitionSize));
		
		
		SaveToPartition(filename, fileHashNum, neededPartition);
		DisplayObject(filename);		
	}
	
	
	static void DownloadCommand(String filename){
		DisplayObject(filename);
	}
	
	static void AddCommand(String disk){
		String assignedDrive;
		int index = 0;
		int numPartitionForEachDrive = (int) numPartitions/(diskList.size() + 1);
		int extra = numPartitionForEachDrive % diskList.size();
		for (int i=0; i < numPartitions; i ++){
			if(numPartitionForEachDrive == 0) {
				break;
			}
			assignedDrive = partitionMapping[i][1];
			if (assignedDrive.equals(diskList.get(index))){
				partitionMapping[i][1] = disk;
				numPartitionForEachDrive--;
				extra--;
			
				if (extra < 0){  //the extra partions will be given up by disk[0]
					if (index == (diskList.size()-1)){
						index = 0;
					} else {
						index++;
					}
				}
			}
		}
		
		diskList.add(disk);
		
		for (int i=0; i < numPartitions; i ++){
			System.out.println("partion: " + i + " " + partitionMapping[i][1] + "   "  + partitionMapping[i][0] );
		}
	}
	
	
	static void RemoveCommand(String disk){
		String assignedDrive;
		int index = 0;
		diskList.remove(disk);
		for (int i=0; i < numPartitions; i ++){
			assignedDrive = partitionMapping[i][1];
			if (assignedDrive.equals(disk)){
				partitionMapping[i][1] = diskList.get(index);

				if (index == (diskList.size()-1)){
					index = 0;
				} else {
					index++;
				}			
			}
		}
		
		
		for (int i=0; i < numPartitions; i ++){
			System.out.println("partion: " + i + " " + partitionMapping[i][1] + "   "  + partitionMapping[i][0] );
		}
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

			partionMemCounter++;

			if (partionMemCounter == numPartionForEachDrive){
				partionMemCounter = 0;
				if (i < ((numPartionForEachDrive*diskList.size())-1)){  //move to the next disk on the list. Partitions not evenly divided (remaining partitions) will be assigned to last disk
					diskIndex ++;
				}
			}
		}
				
	    
	}
	
	static void SaveToPartition (String filename, long hash, int neededPartition){
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
			partitionMapping[partitionIndex][0]=filename;
			partitionMapping[partitionIndex][2]="replicate 1";
			boolean containsItem = fileList.contains(filename);
			if (!containsItem){
				fileList.add(filename);
			}
			String diskSaved = partitionMapping[partitionIndex][1];
			ReplicateToPartition(filename, diskSaved, partitionIndex);
			

			partitionIndex++;
			if (partitionIndex > zeroBasedNumPartition){
				partitionIndex = partitionIndex % zeroBasedNumPartition;
			}
		}
		
	}
	
	
	static void ReplicateToPartition (String filename, String diskSaved, int partitionIndex){
		
	
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
				System.out.println("replicate index: " + replicatePartitionIndex);
				replicatePartitionIndex = replicatePartitionIndex % zeroBasedNumPartition;
				System.out.println("replicate after modulo: " + replicatePartitionIndex);
				partitionValue = partitionMapping[replicatePartitionIndex][0];
				partitionDiskAssignment = partitionMapping[replicatePartitionIndex][1];
			}
	
		}
		partitionMapping[replicatePartitionIndex][0]=filename;
		partitionMapping[replicatePartitionIndex][2]="replicate 2";
		
	}
	
	static void DisplayObject(String filename){
		String partitionValue;
		System.out.println("");
		for (int i=0; i < numPartitions; i ++){
			partitionValue = partitionMapping[i][0];
			if (partitionValue != null && partitionValue.equals(filename)){
				System.out.println(partitionMapping[i][2] + ": partition " + i + " at disk " + partitionMapping[i][1]);
			}
		}
		
	}
	


}
