import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import Common.Packet;

import java.net.SocketTimeoutException;

public class Client {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private boolean rrq;
	private String relativeFilePath;
	private String myName;
	private boolean quite;
	private boolean normal;
	private int timedOutTimes;
	private String serverIPAddressString;






	public String toString(){
		return myName;
	}

	public Client(){
		serverIPAddressString = null;
		timedOutTimes = 0;
		myName = "Client";

		try {
			// Construct a datagram socket and bind it to any available 
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(2000);
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}

	}


	private void makeWriteRequestAndStartSending(String relativeFilePath, String mode, InetAddress address, int port, boolean quite) throws  /*FileNotFoundException ,*/ IOException{


		Path p = Paths.get(relativeFilePath);
		String fileName = p.getFileName().toString();


		InputStream is = null;
		BufferedInputStream in = null;
		try{
			is = new FileInputStream("src/ClientFiles/"+fileName );
			in = new BufferedInputStream(is);
			//the following will be erased
			//File f = new File("src/ClientFiles/"+nameOftheFile);
			//System.out.println(f.isDirectory());
			//System.out.println(f.getParentFile().isDirectory());
			//to here
		}
		catch(FileNotFoundException e){
			System.out.println(e);
			return;
		}





		sendPacket = Packet.makeWriteRequest(relativeFilePath, mode, address, port);
		System.out.println("Making the initial write request.");
		sendReceiveSocket.send(sendPacket);
		if(!quite)
			Packet.displaySendingPacket(sendPacket, toString());

		/*while(true){
			try{
				sendReceiveSocket.send(sendPacket);
				break;
			}
			catch(SocketTimeoutException e){
				timedOutTimes++;
				System.out.println("Timed out waiting for the first response");
				if(timedOutTimes == 6)
					return;
			}
		}
		if(!quite)
			Packet.displaySendingPacket(sendPacket, toString());
		timedOutTimes = 0;*/


		byte[] acknowledgmentByteArray;
		acknowledgmentByteArray = new byte[518];
		receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);
		
		while(true){
			try{
				sendReceiveSocket.receive(receivePacket);
				break;
			}
			catch(SocketTimeoutException e){
				timedOutTimes++;
				System.out.println("Timed out waiting for the first response");
				if(timedOutTimes == 6){
					in.close();
					is.close();
					return;}
			}
		}
		timedOutTimes = 0;
		
		if(!quite)
			Packet.displayReceivedPacket(receivePacket, toString());


		if(Packet.isErrorPacket(receivePacket)){
			in.close();
			is.close();
			return;}

		int connectionManagerPort = receivePacket.getPort();
		InetAddress connectionManagerAddress = receivePacket.getAddress();
		String connectionManagerAddressString = connectionManagerAddress.getHostAddress();


		if(Packet.getBlockNumber(receivePacket) != 0){
			System.out.println("Incorrect Block Number in Acknowledgment");
			Packet.displayReceivedPacket(receivePacket, toString());
			in.close();
			is.close();
			return;
		}
		byte[] data = new byte[512];
		int n;
		int blockNumber = 1;
		boolean emptyFile = true;
		while((n = in.read(data)) != -1){
			emptyFile = false;
			byte[] dataToWrite = new byte[n];
			System.arraycopy(data, 0, dataToWrite, 0, n);
			boolean timedOut = false;
			boolean wrongPacket = false;
			boolean receivedNoInvalidPacket = true;
			sendPacket = Packet.makeDataBlock(blockNumber, dataToWrite, receivePacket.getAddress(), receivePacket.getPort());



			do{



				if(receivedNoInvalidPacket){
					sendReceiveSocket.send(sendPacket);
					if(!quite)
						Packet.displaySendingPacket(sendPacket, toString());}

				acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);



				try{
					sendReceiveSocket.receive(receivePacket);

					if(!quite)
						Packet.displayReceivedPacket(receivePacket, toString());


					if(connectionManagerPort!=receivePacket.getPort() || !connectionManagerAddressString.equals(receivePacket.getAddress().getHostAddress())){
						Packet.ErrorCode error = Packet.ErrorCode.UNKNOWN_TRANSFER_ID;
						System.out.println("Received Something from unknown transfer id");
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						receivedNoInvalidPacket = false;
						continue;

					}


					receivedNoInvalidPacket = true;


					if(Packet.isErrorPacket(receivePacket)){
						in.close();
						is.close();
						return;}

					if(Packet.isInvalidPacket(receivePacket)){
						Packet.ErrorCode error = Packet.determineInvalidPacketType(receivePacket);
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						in.close();
						is.close();

						return;
					}





					if(Packet.getBlockNumber(receivePacket) == Packet.getBlockNumber(sendPacket)){
						timedOut = false;
						wrongPacket = false;
						timedOutTimes = 0;
						break;}
					//System.out.println("The last ACK has a wrong block number");
					wrongPacket = true;
					timedOut = false;}
				catch (SocketTimeoutException e){
					timedOut = true;
					System.out.println("Timed out waiting for acknowledgment.. Resending the last Packet");
					timedOutTimes++;
					if(timedOutTimes == 6){
						in.close();
						is.close();
						return;}
				}}while(timedOut|| wrongPacket || !receivedNoInvalidPacket);







			if (Packet.getBlockNumber(receivePacket) == blockNumber){
				blockNumber++;
			}
			else
				break;

		}

		if(emptyFile){
			byte[] lastblock = new byte[0];
			boolean timedOut = false;
			boolean wrongPacket = false;
			boolean receivedNoInvalidPacket = true;
			sendPacket = Packet.makeDataBlock(blockNumber, lastblock, receivePacket.getAddress(), receivePacket.getPort());




			do{

				if(receivedNoInvalidPacket){
					sendReceiveSocket.send(sendPacket);
					if(!quite)
						Packet.displaySendingPacket(sendPacket, toString());}



				acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);		

				try{
					sendReceiveSocket.receive(receivePacket);
					if(!quite)
						Packet.displayReceivedPacket(receivePacket, toString());







					if(connectionManagerPort!=receivePacket.getPort() || !connectionManagerAddressString.equals(receivePacket.getAddress().getHostAddress())){
						Packet.ErrorCode error = Packet.ErrorCode.UNKNOWN_TRANSFER_ID;
						System.out.println("Received Something from unknown transfer id");
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						receivedNoInvalidPacket = false;
						continue;

					}


					receivedNoInvalidPacket = true;

					if(Packet.isErrorPacket(receivePacket)){
						in.close();
						is.close();
						return;}


					if(Packet.isInvalidPacket(receivePacket)){
						Packet.ErrorCode error = Packet.determineInvalidPacketType(receivePacket);
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						in.close();
						is.close();

						return;
					}



					if(Packet.getBlockNumber(receivePacket) == Packet.getBlockNumber(sendPacket)){
						timedOut = false;
						wrongPacket = false;
						timedOutTimes = 0;
						break;}
					//System.out.println("The last ACK has a wrong block number");
					wrongPacket = true;
					timedOut = false;}
				catch (SocketTimeoutException e){
					timedOut = true;
					System.out.println("Timed out waiting for acknowledgment.. Resending the last Packet");
					timedOutTimes++;
					if(timedOutTimes == 6){
						in.close();
						is.close();
						return;}
				}}while(timedOut || wrongPacket|| !receivedNoInvalidPacket);





		}


		if (sendPacket.getLength() == 516){
			byte[] lastblock = new byte[0];
			boolean timedOut = false;
			boolean wrongPacket = false;
			boolean receivedNoInvalidPacket = true;
			sendPacket = Packet.makeDataBlock(blockNumber, lastblock, receivePacket.getAddress(), receivePacket.getPort());


			do{


				if(receivedNoInvalidPacket){
					sendReceiveSocket.send(sendPacket);
					if(!quite)
						Packet.displaySendingPacket(sendPacket, toString());}


				acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);		
				try{
					sendReceiveSocket.receive(receivePacket);
					if(!quite)
						Packet.displayReceivedPacket(receivePacket, toString());



					if(connectionManagerPort!=receivePacket.getPort() || !connectionManagerAddressString.equals(receivePacket.getAddress().getHostAddress())){
						Packet.ErrorCode error = Packet.ErrorCode.UNKNOWN_TRANSFER_ID;
						System.out.println("Received Something from unknown transfer id");
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						receivedNoInvalidPacket = false;
						continue;

					}


					receivedNoInvalidPacket = true;

					if(Packet.isErrorPacket(receivePacket)){
						in.close();
						is.close();
						return;}


					if(Packet.isInvalidPacket(receivePacket)){
						Packet.ErrorCode error = Packet.determineInvalidPacketType(receivePacket);
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						in.close();
						is.close();

						return;
					}







					if(Packet.getBlockNumber(receivePacket) == Packet.getBlockNumber(sendPacket)){
						timedOut = false;
						wrongPacket = false;
						timedOutTimes = 0;
						break;}
					//System.out.println("The last ACK has a wrong block number");
					wrongPacket = true;
					timedOut = false;}
				catch (SocketTimeoutException e){
					timedOut = true;
					System.out.println("Timed out waiting for acknowledgment.. Resending the last Packet");
					timedOutTimes++;
					if(timedOutTimes == 6){
						in.close();
						is.close();
						return;}
				}}while(timedOut || wrongPacket || !receivedNoInvalidPacket);




		}


		System.out.println("about to close the output streams..");
		in.close();
		is.close();
		System.out.println("Finished writing to server");
	}


	private void makeReadRequestAndStartReceiveing(String relativeFilePath, String mode, InetAddress address, int port, boolean quite) 
			throws /*FileNotFoundException,*/ IOException{
		OutputStream os = null;
		BufferedOutputStream out = null;

		Path p = Paths.get(relativeFilePath);
		String fileName = p.getFileName().toString();


		try{
			os = new FileOutputStream("src/ClientFiles/"+fileName);
			out = new BufferedOutputStream(os);
			//the following will be erased
			//File f = new File("src/ClientFiles/"+nameOftheFile);
			//System.out.println(f.isDirectory());
			//System.out.println(f.getParentFile().isDirectory());
			//to here
		}
		catch(FileNotFoundException e){
			System.out.println(e);
			return;
		}


		sendPacket = Packet.makeReadRequest(relativeFilePath, mode, address, port);
		sendReceiveSocket.send(sendPacket);
		System.out.println("Making the inital read request.");
		if(!quite)
			Packet.displaySendingPacket(sendPacket, toString());

		byte[] data = new byte[518];
		int blockNumber = 1;
		boolean iHaveReceivedtheLastDataPacket = false;
		receivePacket = new DatagramPacket(data, data.length);

		int connectionManagerPort = 0;
		InetAddress connectionManagerAddress = null;
		String connectionManagerAddressString = null;
		boolean firstTime = true;


		while(true){


			// Accounts for the last data block..
			if(iHaveReceivedtheLastDataPacket){
				try{
					sendReceiveSocket.receive(receivePacket);
					if(!quite)
						Packet.displayReceivedPacket(receivePacket, toString());
				}
				catch(SocketTimeoutException e){
					break;
				}

				if(Packet.getBlockNumber(receivePacket) < (blockNumber-1))
					break;
			}

			else{
				try{
					sendReceiveSocket.receive(receivePacket);
					if(!quite)
						Packet.displayReceivedPacket(receivePacket, toString());

					if(firstTime){
						connectionManagerPort = receivePacket.getPort();
						connectionManagerAddress = receivePacket.getAddress();
						connectionManagerAddressString = connectionManagerAddress.getHostAddress();
						firstTime = false;
					}


					if(connectionManagerPort!=receivePacket.getPort() || !connectionManagerAddressString.equals(receivePacket.getAddress().getHostAddress())){
						Packet.ErrorCode error = Packet.ErrorCode.UNKNOWN_TRANSFER_ID;
						System.out.println("Received Something from unknown transfer id");
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						continue;

					}

					if(Packet.isErrorPacket(receivePacket)){
						out.close();
						os.close();
						return;
					}

					if(Packet.isInvalidPacket(receivePacket)){
						Packet.ErrorCode error = Packet.determineInvalidPacketType(receivePacket);
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!quite)
							Packet.displaySendingPacket(errorPacket, toString());

						out.close();
						os.close();

						return;
					}





				}
				catch(SocketTimeoutException e){
					System.out.println("Timed out waiting for the data block..");
					System.out.println("");
					/*+" Resending the last Right ACK BLOCK");
	    			sendPacket = Packet.makeRealAcknowledgment(blockNumber-1, receivePacket.getAddress(), receivePacket.getPort());
	    			sendReceiveSocket.send(sendPacket);*/
					timedOutTimes++;
					if(timedOutTimes == 6){
						out.close();
						os.close();
						return;
					}
					continue;
				}

			}


			if(blockNumber == Packet.getBlockNumber(receivePacket)){


				//Packet.displayReceivedPacket(receivePacket, "connectionManager");

				if(!iHaveReceivedtheLastDataPacket){
					if(!quite)
						System.out.println("received the Right data block "+(blockNumber)+" Sending the right ack back");

					timedOutTimes = 0;
					blockNumber++;

					if(!quite)
						System.out.println("Writing the right data block into the file");

					byte[] dataToWrite = new byte[receivePacket.getLength() -4];
					System.arraycopy(data, 4, dataToWrite, 0, receivePacket.getLength() -4);
					out.write(dataToWrite);}

				else{
					System.out.println("received the Right last data block "+(blockNumber)+" but unable to write it since its already written");
				}

			}
			else
			{	if(!quite)
				System.out.println("received the wrong block number " +Packet.getBlockNumber(receivePacket)+""
						+ " but will still send the ack for this packet");

			}


			sendPacket = Packet.makeRealAcknowledgment(Packet.getBlockNumber(receivePacket), receivePacket.getAddress(), receivePacket.getPort());
			sendReceiveSocket.send(sendPacket);
			if(!quite)
				Packet.displaySendingPacket(sendPacket, toString());

			if(receivePacket.getLength() < 516 &&  (Packet.getBlockNumber(receivePacket) == blockNumber -1))
				iHaveReceivedtheLastDataPacket = true;
		}

		System.out.println("about to close the output streams..");
		out.close();
		os.close();
		System.out.println("finished reading the file from the Server");
	}

	public void sendAndReceive()
	{
		Scanner reader;
		reader = new Scanner(System.in);
		String userInput;
		InetAddress serverIPAddress = null;

		while(true){
			timedOutTimes = 0;

			System.out.println("To quit the client program, press 'quit' at any time.");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			System.out.println("Please enter 1 to operate in quite or 2 for verbose mode.");

			while(true){
				userInput = reader.nextLine();

				if(userInput.equals("quit")){
					reader.close();
					return;}

				if(userInput.equals("1") || userInput.equals("2"))
				{
					if(userInput.equals("1"))
						quite = true;

					else
						quite = false;
					break;
				}

				else
					System.out.println("Please enter 1 for quite or 2 for verbose mode!!!");
			}


			//is it a normal mode or test mode
			System.out.println("Please enter 1 to operate normal mode or 2 for test mode");
			while(true){
				userInput = reader.nextLine();

				if(userInput.equals("quit")){
					reader.close();
					return;}

				if(userInput.equals("1") || userInput.equals("2"))
				{
					if(userInput.equals("1"))
						normal = true;

					else
						normal = false;
					break;
				}

				else
					System.out.println("Please enter 1 for normal mode or 2 for test mode!!!");
			}


			int port;
			if(normal){
				boolean sameComputer;

				System.out.println("Is the Server running on the same computer");
				System.out.println("1: Yes");
				System.out.println("2: No");
				while(true){
					userInput = reader.nextLine();
					if(userInput.equals("quit")){
						reader.close();
						return;}

					if(userInput.equals("1") || userInput.equals("2"))
					{
						if(userInput.equals("1"))
							sameComputer = true;
						else
							sameComputer = false;
						break;
					}
					else{
						System.out.println("Select the right option!");
						System.out.println("1: Yes");
						System.out.println("2: No");
					}
				}

				if(!sameComputer){
					System.out.println("Please enter the IP address of the server you want to connect to");
					userInput = reader.nextLine();

					if(userInput.equals("quit")){
						reader.close();
						return;}

					serverIPAddressString = userInput;
					try {
						serverIPAddress = InetAddress.getByName(serverIPAddressString);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				else{
					try {
						serverIPAddress = InetAddress.getLocalHost();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}


				port = 69;
			}


			else{
				System.out.println("You selected Test mode. Make sure that the error simulator is "
						+ "set up and ready to receive the request from the client");
				try {
					serverIPAddress = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				port = 23;}


			System.out.println("Please enter 1 for read request or 2 for write request.");
			while(true){

				userInput = reader.nextLine();

				if(userInput.equals("quit")){
					reader.close();
					return;}


				if(userInput.equals("1") || userInput.equals("2"))
				{
					if(userInput.equals("1"))
						rrq = true;

					else
						rrq = false;
					break;
				}

				else
					System.out.println("Please enter 1 for read request or 2 for write request!!");
			}

			System.out.println("Please enter (name of the file with its relative path on Server) that you want to read or write");
			userInput = reader.nextLine();

			if(userInput.equals("quit")){
				reader.close();
				return;}

			relativeFilePath = userInput;


			if(rrq){ 	 
				try {
					makeReadRequestAndStartReceiveing(relativeFilePath, "octet", serverIPAddress, port, quite);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			else{
				try {
					makeWriteRequestAndStartSending(relativeFilePath,"octet", serverIPAddress, port, quite);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(timedOutTimes == 6){
				System.out.println(toString()+": Nothing received after 6 timeouts.");
			}

			System.out.println("Transfer completed");

			sendReceiveSocket.disconnect();
			try {
				sendReceiveSocket = new DatagramSocket();
				sendReceiveSocket.setSoTimeout(2000);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}






	public static void main(String args[])
	{

		Client c = new Client();
		c.sendAndReceive();
		System.out.println("Client has shut down");

	}

}
