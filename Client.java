import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	public static void main(String[] args){
		
		if (args.length < 2) {
			System.out.println("There should be at least two arguments: the server's name/IP address and a port number." );
			System.exit(1);
		}
		  
		int port=0;
		
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException err){
			System.out.println("The second argument must be an integer.");
			System.exit(1);
		}
		  
		String servername = args[0];
		String command = Display();
		command = command.trim();
		

		while ((command.indexOf(' ') == -1) || (((command.startsWith("add") || command.startsWith("remove") || command.startsWith("upload") || command.startsWith("download") || command.startsWith("delete"))) && (command.indexOf('/') == -1))){
			System.out.println("");
			System.out.println("Invalid command format: " + command);
			command = Display();
			command = command.trim();
		}
	
		try {
			ConnectToServer(servername, port, command);
		} catch (Exception ex){
			System.out.println("Exception thrown  :" + ex);
		}    
	   
	}// end main
	
	
	
	static String Display(){
		Scanner in = new Scanner(System.in);
		
		System.out.println("The following commands are available:");
		System.out.println("	download <user/object>");
		System.out.println("	list <user>");
		System.out.println("	upload <user/object>");
		System.out.println("	delete <user/object>");
		System.out.println("	add <disk/port>");
		System.out.println("	remove <disk/port>");
		System.out.print("Enter a command: ");
	    String s = in.nextLine();
	    return s;
	}
	
	static void ConnectToServer(String servername, int port, String command) throws Exception{
		
        String commandArg = (command.substring(command.indexOf(' ') + 1)).trim();
        String commandKeyword = (command.substring(0, command.indexOf(' '))).trim().toLowerCase();
        
        //String username = "";
    	String filename = "";
    	//String newfilename = "";
    	String strPort;

    	
    	if (commandKeyword.startsWith("upload") || commandKeyword.startsWith("download") || commandKeyword.startsWith("delete")){
        	//String username = (commandArg.substring(0,commandArg.indexOf('/'))).trim();
        	filename = (commandArg.substring(commandArg.indexOf('/') + 1)).trim();
        	
        }
    	
    	if (commandKeyword.startsWith("add") || commandKeyword.startsWith("remove")){
        	strPort = (commandArg.substring(commandArg.indexOf('/') + 1)).trim();
    		int intPort=0;
    		
    		try {
    			intPort = Integer.parseInt(strPort);
    		} catch (NumberFormatException err){
    			System.out.println("Error: Entered port is not an integer.");
    			System.exit(1);
    		}
        	
        }
    	
    	if (commandKeyword.startsWith("upload")) {
    		File checkFile = new File(filename);
    		if (!checkFile.exists()){
    			System.out.println("File " + filename + " does not exists");
    			System.exit(1);
    		}	
    	}
    	

  
		Socket sock = new Socket (servername, port);	
		
        OutputStream os = sock.getOutputStream();	    
        //Sending file name and file size to the server
        DataOutputStream dos = new DataOutputStream(os);   
        dos.writeUTF(command);
        
    	

		
		if (command.startsWith("upload")) {
			File myFile = new File(filename);
			byte[] mybytearray = new byte[(int) myFile.length()];        
			FileInputStream fis = new FileInputStream(myFile);
			BufferedInputStream bis = new BufferedInputStream(fis);         
			DataInputStream dis = new DataInputStream(bis);   
			dis.readFully(mybytearray, 0, mybytearray.length);
			dos.writeLong(mybytearray.length);   
			dos.write(mybytearray, 0, mybytearray.length); 
			dis.close();

		}
      
        dos.flush();  

        //Closing socket
       
        dos.close();
        os.close();
        sock.close();
		
	}
}
