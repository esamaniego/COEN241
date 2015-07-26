import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class SServer {
	
	public static void main (String[] args) throws IOException {
		
		int port = findPort();
		System.out.println("Port number is: " + port);
		System.out.println("");
		
		ServerSocket serverSocket = new ServerSocket(port);
		int bytesRead;
		//int fileSize=0;
		
		while(true){
			Socket clientSocket = null;
	        clientSocket = serverSocket.accept();
	        InputStream in = clientSocket.getInputStream();
	        DataInputStream clientData = new DataInputStream(in);
	        OutputStream os = clientSocket.getOutputStream();
	        DataOutputStream dos = new DataOutputStream(os);
	        
	        String command = clientData.readUTF(); 
	        String commandArg = (command.substring(command.indexOf(' ') + 1)).trim();
	        command = (command.substring(0, command.indexOf(' '))).trim().toLowerCase();
	        
	        if (command.startsWith("upload")){ 
		        OutputStream output = new FileOutputStream("/tmp/" + commandArg);
		        long size = clientData.readLong();
		        //fileSize = (int)size;
		        byte[] buffer = new byte[1024];   
		        while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1)   
		        {   
		            output.write(buffer, 0, bytesRead);   
		            size -= bytesRead;   
		        }
		        output.close();
		        System.out.println("File " + commandArg + " copied from server");
		        System.out.println("");
	        }//end upload
	        
	        else if (command.startsWith("download")){
	        	File myFile = new File("/tmp/" + commandArg);
	        	byte[] mybytearray = new byte[(int) myFile.length()];        
		        FileInputStream fis = new FileInputStream(myFile);
		        BufferedInputStream bis = new BufferedInputStream(fis);         
		        DataInputStream dis = new DataInputStream(bis);   
		        dis.readFully(mybytearray, 0, mybytearray.length);
		        dos.writeLong(mybytearray.length);   
		        dos.write(mybytearray, 0, mybytearray.length);     
		        dis.close();
		        System.out.println("File " + commandArg + " sent to server");
		        System.out.println("");
	        }//end else download
	        
	        else if (command.startsWith("delete")){ 
	        	File myFile = new File("/tmp/" + commandArg);
	        	
	        	if(myFile.delete()){
	    			System.out.println(myFile.getName() + " is deleted!");
	    		}else{
	    			System.out.println("Delete operation is failed.");
	    		}
	        	System.out.println("");
	        }//end else delete
	       
	        clientData.close();
	        in.close();
	        
			
		}//end while
		
	}//end main

	
	
	
	
	
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

}
