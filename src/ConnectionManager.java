import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.net.SocketTimeoutException;
import java.io.File;


import Common.Packet;



public class ConnectionManager implements Runnable{

	private DatagramPacket receivePacket,  sendPacket;
	private DatagramSocket sendReceiveSocket;
	private Boolean quite[];
	private int timedOutTimes;


	@Override
	public void run() {
		//TODO Auto-generated method stub
		System.out.println("I  am "+Thread.currentThread().getName());

		/*
		if(Packet.isInvalidPacket(receivePacket)){
			System.out.println("Error Happaned on request: "+Packet.determineInvalidPacketType(receivePacket));
			System.out.println("Type: "+Packet.getPacketTypeString(receivePacket));
		}*/


		if(!Packet.isRequestValid(receivePacket)){
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
			
			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}


			//System.out.println(toString()+" has shut down");
			//return;
		}


		else{
			if(Packet.isReadRequest(receivePacket)){

				try {
					startReadingAndSending(receivePacket);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else{

				try {
					startReceivingAndWriting(receivePacket);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		System.out.println(toString()+" has shut down");


	}


	public String toString(){
		return Thread.currentThread().getName();
	}



	public ConnectionManager(DatagramPacket receivePacket, Boolean quite[]){
		this.receivePacket = receivePacket;
		this.quite = quite;
		timedOutTimes = 0;

		try {
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(2000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	private void startReadingAndSending(DatagramPacket receivePacket) throws FileNotFoundException, IOException{


		File f = new File("src/ServerFiles/"+Packet.getFileNameFromARequest(receivePacket));
		
		if(f.exists() && f.isDirectory()){
			Packet.ErrorCode error = Packet.ErrorCode.FILENAME_IS_DIRECTORY;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}
		
		
		
		if(!f.exists() && !f.isDirectory() ){
			Packet.ErrorCode error = Packet.ErrorCode.FILE_NOT_FOUND;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}
		


		if(f.length()>=65534){
			Packet.ErrorCode error = Packet.ErrorCode.FILE_SIZE_NOT_SUPPORTED;

			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;

		}




		InputStream is = new FileInputStream("src/ServerFiles/"+Packet.getFileNameFromARequest(receivePacket));
		BufferedInputStream in =  new BufferedInputStream(is);

		int clientPort = receivePacket.getPort();
		InetAddress clientAddress = receivePacket.getAddress();
		String clientAddressString = clientAddress.getHostAddress();
		


		byte[] data = new byte[512];
		int n;
		int blockNumber = 1;
		boolean emptyFile = true;
		while ((n = in.read(data)) != -1) {
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
					synchronized(quite){
					if(!quite[0])
						Packet.displaySendingPacket(sendPacket, toString());}
				}
				byte[] acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);

				
				
				try{
					sendReceiveSocket.receive(receivePacket);
					
					
					synchronized(quite){
					if(!quite[0])
						Packet.displayReceivedPacket(receivePacket, toString());}


					if(clientPort!=receivePacket.getPort() || !clientAddressString.equals(receivePacket.getAddress().getHostAddress()) ){


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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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
						System.out.println(toString()+": Nothing received after 6 timeouts.");
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
					synchronized(quite){
					if(!quite[0])
						Packet.displaySendingPacket(sendPacket, toString());}}



				byte[] acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);		

				try{
					sendReceiveSocket.receive(receivePacket);
					synchronized(quite){
					if(!quite[0])
						Packet.displayReceivedPacket(receivePacket, toString());}


					if(clientPort!=receivePacket.getPort() || !clientAddressString.equals(receivePacket.getAddress().getHostAddress()) ){


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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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
						System.out.println(toString()+": Nothing received after 6 timeouts.");
						return;}
				}}while(timedOut || wrongPacket || !receivedNoInvalidPacket);
		}



		if (sendPacket.getLength() == 516 ){
			byte[] lastblock = new byte[0];
			boolean timedOut = false;
			boolean wrongPacket = false;
			boolean receivedNoInvalidPacket = true;
			sendPacket = Packet.makeDataBlock(blockNumber, lastblock, receivePacket.getAddress(), receivePacket.getPort());




			do{

				if(receivedNoInvalidPacket){
					sendReceiveSocket.send(sendPacket);
					
					synchronized(quite){
					if(!quite[0])
						Packet.displaySendingPacket(sendPacket, toString());}}
				
				byte[] acknowledgmentByteArray = new byte[518];
				receivePacket = new DatagramPacket(acknowledgmentByteArray, acknowledgmentByteArray.length);		

				try{
					sendReceiveSocket.receive(receivePacket);
					
					synchronized(quite){
					if(!quite[0])
						Packet.displayReceivedPacket(receivePacket, toString());}

				
					
					if(clientPort!=receivePacket.getPort() || !clientAddressString.equals(receivePacket.getAddress().getHostAddress()) ){


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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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
						System.out.println(toString()+": Nothing received after 6 timeouts.");
						return;}
				}}while(timedOut || wrongPacket || !receivedNoInvalidPacket);}

		in.close();
		is.close();
		System.out.println("finished sending the file to client!!");
	}

	private void startReceivingAndWriting(DatagramPacket receivePacket)throws FileNotFoundException, IOException{

		File f = new File("src/ServerFiles/"+Packet.getFileNameFromARequest(receivePacket));
	
		
		//File fDirectory = new File(f.getParentFile().getName());
		File fDirectory = new File(f.getParent());
		System.out.println("Parent Directory Directory: "+fDirectory.toString());
		System.out.println("exists: "+ fDirectory.exists());
		System.out.println("Parent Directory isDirectory: "+ fDirectory.isDirectory());
		System.out.println("Parent Directory isNotWritable: "+ !fDirectory.canWrite());
		System.out.println("The actual file isNotWritable: "+ !f.canWrite());
		
		File imediateDirectory = new File(f.getParentFile().getName());
		System.out.println("Imediate Directory:" +imediateDirectory.toString());
		
		
		if(!fDirectory.exists()){
			
			Packet.ErrorCode error = Packet.ErrorCode.FILE_NOT_FOUND;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}
		/*
		if(fDirectory.exists() && fDirectory.isDirectory() && !f.canWrite()){
			Packet.ErrorCode error = Packet.ErrorCode.ACCESS_VIOLATION;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}*/
		
		if(f.exists() && !f.isDirectory() ){
			Packet.ErrorCode error = Packet.ErrorCode.FILE_ALREADY_EXIST;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}
		
		
		OutputStream os = null;
		BufferedOutputStream out = null;
		try{
		os = new FileOutputStream("src/ServerFiles/"+Packet.getFileNameFromARequest(receivePacket));
		out = new BufferedOutputStream(os);}
		catch(IOException e){
			Packet.ErrorCode error = Packet.ErrorCode.ACCESS_VIOLATION;
			
			int errorCode = error.getErrorCode();
			String errorMsg = error.getDescription();
			DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

			try {
				sendReceiveSocket.send(errorPacket);
			} catch (IOException e11) {
				// TODO Auto-generated catch block
				e11.printStackTrace();
			}

			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(errorPacket, toString());}

			return;
		}

		int clientPort = receivePacket.getPort();
		InetAddress clientAddress = receivePacket.getAddress();
		String clientAddressString = clientAddress.getHostAddress();

		sendPacket = Packet.makeRealAcknowledgment(0, receivePacket.getAddress(), receivePacket.getPort());
		sendReceiveSocket.send(sendPacket);
		
		synchronized(quite){
		if(!quite[0])
			Packet.displaySendingPacket(sendPacket, toString());}

		byte[] data = new byte[518];
		int blockNumber = 1;
		boolean iHaveReceivedtheLastDataPacket = false;
		receivePacket = new DatagramPacket(data, data.length);



		while(true){


			// Accounts for the last data block..
			if(iHaveReceivedtheLastDataPacket){
				try{
					sendReceiveSocket.receive(receivePacket);
					
					synchronized(quite){
					if(!quite[0])
						Packet.displayReceivedPacket(receivePacket, toString());}
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
					
					synchronized(quite){
					if(!quite[0]){
						Packet.displayReceivedPacket(receivePacket, toString());
						System.out.println("");}}


					if(clientPort!=receivePacket.getPort() || !clientAddressString.equals(receivePacket.getAddress().getHostAddress()) ){


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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

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
						System.out.println(toString()+": Nothing received after 6 timeouts.");
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
					
					synchronized(quite){
					if(!quite[0])
						System.out.println("received the Right data block "+(blockNumber)+" Sending the right ack back");}

					timedOutTimes = 0;
					blockNumber++;

					synchronized(quite){
					if(!quite[0])	
						System.out.println("Writing the right data block into the file");}

					byte[] dataToWrite = new byte[receivePacket.getLength() -4];
					System.arraycopy(data, 4, dataToWrite, 0, receivePacket.getLength() -4);
					
					
					try{
						out.write(dataToWrite);
					}catch(IOException e){
						
						Packet.ErrorCode error = Packet.ErrorCode.DISK_FULL;
						
						int errorCode = error.getErrorCode();
						String errorMsg = error.getDescription();
						DatagramPacket errorPacket = Packet.makeErrorCodePacket(receivePacket.getAddress(), receivePacket.getPort(), errorCode, errorMsg);

						try {
							sendReceiveSocket.send(errorPacket);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						synchronized(quite){
						if(!quite[0])
							Packet.displaySendingPacket(errorPacket, toString());}

						return;
					}
				
				}

				else{
					synchronized(quite){
					if(!quite[0])
						System.out.println("received the Right last data block "+(blockNumber)+" but unable to write it since its already written");}
				}

			}
			else
			{
				System.out.println("received the wrong block number " +Packet.getBlockNumber(receivePacket)+""
						+ " but will still send the ack for this packet");

			}


			sendPacket = Packet.makeRealAcknowledgment(Packet.getBlockNumber(receivePacket), receivePacket.getAddress(), receivePacket.getPort());
			sendReceiveSocket.send(sendPacket);
			
			synchronized(quite){
			if(!quite[0])
				Packet.displaySendingPacket(sendPacket, toString());}

			if(receivePacket.getLength() < 516 &&  (Packet.getBlockNumber(receivePacket) == blockNumber -1))
				iHaveReceivedtheLastDataPacket = true;

		}
		System.out.println("about to close the output streams..");
		out.close();
		os.close();
		System.out.println("finished reading the file from the Client");

	}




	/*
	public static void main(String[] args){

		DatagramPacket check = null;
		DatagramPacket check2 = null;

		try {
			 check = Packet.makeWriteRequest("test.txt", "octet", InetAddress.getLocalHost(), 1025);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			check2 = Packet.makeRealAcknowledgment(64, InetAddress.getLocalHost(), 1026);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		System.out.println(Packet.getFileNameFromARequest(check));
		System.out.println(Packet.getBlockNumber(check2));
	}*/

}
