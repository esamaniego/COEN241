mport java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
		System.out.println("	add <disk>");
		System.out.println("	remove <disk>");
		System.out.print("Enter a command: ");
	    String s = in.nextLine();
	    return s;
	}
	
	static void ConnectToServer(String servername, int port, String command) throws Exception{
  
		Socket sock = new Socket (servername, port);	
		
        OutputStream os = sock.getOutputStream();	    
        //Sending file name and file size to the server
        DataOutputStream dos = new DataOutputStream(os);   
        dos.writeUTF(command);
        
        String commandArg = (command.substring(command.indexOf(' ') + 1)).trim();
        command = (command.substring(0, command.indexOf(' '))).trim().toLowerCase();
		
		if (command.startsWith("upload")) {
			File myFile = new File(commandArg);
		    byte[] mybytearray = new byte[(int) myFile.length()];        
	        FileInputStream fis = new FileInputStream(myFile);
	        BufferedInputStream bis = new BufferedInputStream(fis);         
	        DataInputStream dis = new DataInputStream(bis);   
	        dis.readFully(mybytearray, 0, mybytearray.length);
	        dos.writeLong(mybytearray.length);   
	        dos.write(mybytearray, 0, mybytearray.length); 
		}
      
        dos.flush();    
        //Closing socket
        dos.close();
        os.close();
        sock.close();
		
	}
}

