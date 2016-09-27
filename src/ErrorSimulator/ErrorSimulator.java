package ErrorSimulator;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import Common.Packet;


import java.util.ArrayList;
import java.util.Arrays; 


public class ErrorSimulator
{


	private DatagramPacket movingPacket;
	private DatagramSocket receiveSocket23, sendReceiveSocketServer, sendReceiveSocketClient;
	private Boolean[] quite;
	private volatile Integer[] threadEndCount;
	private boolean quit;
	private ArrayList<NetworkErrorSpecs> networkErrors;
	private ErrorSimulateClass errorSimulate;
	private Boolean[] errorCode5;
	private int errorCode5BlockNumber;
	private Scanner userInput;
	private boolean errorSimulatedonRequest;
	private InetAddress serverIPAddress;
	private String serverIPAddressString;


	/**
	 * Constructor for objects of class IntermediateHost
	 */
	public ErrorSimulator()
	{
		//lastDataBlockSentNumber = 0;
		quit = false;
		quite = new Boolean[1];
		errorCode5 = new Boolean[2];
		errorCode5BlockNumber = 0;
		threadEndCount = new Integer[1];
		userInput = new Scanner(System.in);
		try {

			receiveSocket23 = new DatagramSocket(23);
			sendReceiveSocketServer = new DatagramSocket();
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
		threadEndCount[0] = 0;
		errorSimulatedonRequest = false;
	}

	@Override
	public String toString(){
		return "ErrorSimulator";
	}



	public void sendAndReceive(){


		while(true){



			if(quit){
				userInput.close();
				break;
			}
			int clientPort;
			InetAddress clientAddress;
			int connectionManagerPort;
			InetAddress connectionManagerAddress;
			threadEndCount[0] = 0;
			errorCode5[0] = false;
			errorCode5[1] = null;
			errorCode5BlockNumber = 0;
			networkErrors = new ArrayList<NetworkErrorSpecs>();
			errorSimulatedonRequest = false;


			errorSimulate = takeInitialUserInputAndGetErrorTYPE(networkErrors, quite, errorCode5);


			//used for missing mode or missing fileName
			if(errorSimulate!=null)
				if(errorSimulate.getPacketType()== Packet.PacketType.REQ){
					if(errorSimulate.getErrorType() == Packet.ErrorCode.INVALID_OPCODE || 
							errorSimulate.getErrorType() == Packet.ErrorCode.MISSING_FILENAME ||
							errorSimulate.getErrorType() == Packet.ErrorCode.MISSING_MODE)
						errorSimulatedonRequest = true;

				

					else{}
				}



			byte data[] = new byte[516];
			movingPacket = new DatagramPacket(data, data.length);
			System.out.println("Error Simulator: Waiting for Packet from the Client");
			try{
				receiveSocket23.receive(movingPacket);
			} catch (IOException e){
				System.out.print("IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			clientPort = movingPacket.getPort();
			clientAddress = movingPacket.getAddress();
			if(!quite[0]){
				Packet.displayReceivedPacket(movingPacket, toString());
				System.out.println("");}



			
			
			if(errorSimulatedonRequest){
				errorSimulate.simulateError(movingPacket);
			}


			movingPacket.setPort(69);
			movingPacket.setAddress(serverIPAddress);
			if(!quite[0])
				Packet.displaySendingPacket(movingPacket, toString());
			try {
				sendReceiveSocketServer.send(movingPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Error: Packet sent to Server on port 69.\n");
			System.out.println("");




			data = new byte[516];
			movingPacket = new DatagramPacket(data, data.length);
			try {
				// Block until a datagram is received via sendReceiveSocket.
				//System.out.println("Waiting..."); 
				sendReceiveSocketServer.receive(movingPacket);
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			connectionManagerPort = movingPacket.getPort();
			connectionManagerAddress = movingPacket.getAddress();
			if(!quite[0]){
				Packet.displayReceivedPacket(movingPacket, toString());
				System.out.println("");}







			try {
				sendReceiveSocketClient = new DatagramSocket();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			movingPacket.setPort(clientPort);
			movingPacket.setAddress(clientAddress);
			try {
				sendReceiveSocketClient.send(movingPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(!quite[0]){
				Packet.displaySendingPacket(movingPacket, toString());
				System.out.println("");}

			
			if(!errorSimulatedonRequest){

			Thread serverSidedListener_ClientSidedSender = new Thread(new Messenger( sendReceiveSocketServer, sendReceiveSocketClient, 
					clientAddress, clientPort, networkErrors, threadEndCount, quite, errorSimulate),"serverSidedListener_ClientSidedSender");

			Thread clientSidedListener_ServerSidedSender = new Thread(new Messenger(sendReceiveSocketClient, sendReceiveSocketServer,
					connectionManagerAddress, connectionManagerPort, networkErrors, threadEndCount, quite, errorSimulate),"clientSidedListener_ServerSidedSender");



			clientSidedListener_ServerSidedSender.start();
			serverSidedListener_ClientSidedSender.start();

			//BUSY WAITING
			while(threadEndCount[0]!=2)
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			}
			
			String enteredLine;
			System.out.println("The transfer has finished. Do you want to quite now?");
			System.out.println("1: Yes");
			System.out.println("2: No");

			while(true){

				enteredLine = userInput.nextLine();
				if(enteredLine.equals("1") || enteredLine.equals("2"))
				{
					if(enteredLine.equals("1"))
						quit = true;
					else
						quit = false;
					break;
				}
				else{
					System.out.println("Select the right option!");
					System.out.println("The transfer has finished. Do you want to quite now?");
					System.out.println("1: Yes");
					System.out.println("2: No");
				}
			}



		}  


	}





	public void testErrorSimulate(){
		ErrorSimulateClass x = new ErrorSimulateClass(Packet.ErrorCode.LONG_ACK, Packet.PacketType.ACK, 5);
		DatagramPacket y = null;
		DatagramPacket z = null;
		try {
			byte[] something = new byte[]{1, 2, 3};
			y = Packet.makeDataBlock(5, something, InetAddress.getLocalHost(), 1025);
			z = Packet.makeRealAcknowledgment(5, InetAddress.getLocalHost(), 1025);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Y Before");
		System.out.println(Arrays.toString(y.getData()));
		y = x.simulateError(y);
		System.out.println("Y After");
		System.out.println(Arrays.toString(y.getData()));
		System.out.println("Z before");
		System.out.println(Arrays.toString(z.getData()));
		z = x.simulateError(z);
		System.out.println("Z After");
		System.out.println(Arrays.toString(z.getData()));
	}





	/*
	public DatagramPacket corruptedPacket(DatagramPacket packet, Packet.ErrorCode error){
		//DatagramPacket packet = packet;
		if (error == Packet.ErrorCode.MISSING_FILENAME){

		}


		return packet;
	}*/



	public ErrorSimulateClass takeInitialUserInputAndGetErrorTYPE(ArrayList<NetworkErrorSpecs> networkErrors, Boolean[] quite,
			Boolean[] errorCode5){


		boolean weNeedNetworkErrors;
		int numPackets = 0;



		String enteredLine;
		ErrorSimulateClass simulatedError = null;


		System.out.println("Which mode would you like to operate in?");
		System.out.println("1: Quite mode");
		System.out.println("2: Verbose mode");
		while(true){
			enteredLine = userInput.nextLine();
			if(enteredLine.equals("1") || enteredLine.equals("2"))
			{
				if(enteredLine.equals("1"))
					quite[0] = true;
				else
					quite[0] = false;
				break;
			}
			else{
				System.out.println("Select the right option!");
				System.out.println("1: Quite mode");
				System.out.println("2: Verbose mode");}
		}
		
		
		
		boolean sameComputer;
		System.out.println("Is the Server running on the same computer");
		System.out.println("1: Yes");
		System.out.println("2: No");
		while(true){
			enteredLine = userInput.nextLine();
			if(enteredLine.equals("1") || enteredLine.equals("2"))
			{
				if(enteredLine.equals("1"))
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
		
		if(sameComputer){
			try {
				serverIPAddress = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		else{
			System.out.println("Please enter the IP address of the server you want to connect to");
			enteredLine = userInput.nextLine();
			serverIPAddressString = enteredLine;
			
			try {
				serverIPAddress = InetAddress.getByName(serverIPAddressString);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}



		System.out.println("Do you want to delay, loose or delay Packets?");
		System.out.println("1: Yes");
		System.out.println("2: No");
		while(true){
			enteredLine = userInput.nextLine();
			if(enteredLine.equals("1") || enteredLine.equals("2"))
			{
				if(enteredLine.equals("1"))
					weNeedNetworkErrors = true;
				else
					weNeedNetworkErrors = false;
				break;
			}
			else{
				System.out.println("Select the right option!");
				System.out.println("Do you want to delay, loose or duplicate Packets?");
				System.out.println("1: Yes");
				System.out.println("2: No");
			}
		}



		if(weNeedNetworkErrors){
			boolean numberNotEntered = true;
			while(numberNotEntered){
				System.out.println("Please enter the total number of Packets you want to either delay, lose or duplicate i.e Network Error");
				System.out.println("The number must be equal to or greater than 0");
				try {
					numPackets = Integer.parseInt(userInput.nextLine());
					if(numPackets>=0)
						numberNotEntered = false;
				} catch (NumberFormatException e) {}
			}
			for (int i = 0 ; i < numPackets ; i++){

				int nthPacket = 0;
				Packet.PacketType nthPacketType;
				NetworkErrorType nthPacketNetworkErrorType;

				numberNotEntered = true;
				while(numberNotEntered){
					System.out.println("Please enter the block number of the packet that you want to put Network Error on");
					System.out.println("The number must be greater than 1");
					try {
						nthPacket = Integer.parseInt(userInput.nextLine());
						if(nthPacket>1)
							numberNotEntered = false;
					} catch( NumberFormatException e){}
				}




				System.out.println("Please select one of the follwing options for the "+nthPacket+"th packet");
				System.out.println("1: ACK");
				System.out.println("2: DATA");
				while(true){
					enteredLine = userInput.nextLine();
					if(enteredLine.equals("1") || enteredLine.equals("2"))
					{
						if(enteredLine.equals("1"))
							nthPacketType = Packet.PacketType.ACK;
						else
							nthPacketType = Packet.PacketType.DATA;

						break;
					}
					else{
						System.out.println("Select the right option from the following!");
						System.out.println("Please select one of the follwing options for the "+nthPacket+"th packet");
						System.out.println("1: ACK");
						System.out.println("2: DATA");

					}
				}







				System.out.println("Please select one of the follwing options for the "+nthPacket+"th "+nthPacketType+" packet");
				System.out.println("1: DELAY");
				System.out.println("2: DUPLICATE");
				System.out.println("3: LOST");
				while(true){
					enteredLine = userInput.nextLine();
					if(enteredLine.equals("1") || enteredLine.equals("2") || enteredLine.equals("3"))
					{
						if(enteredLine.equals("1"))
							nthPacketNetworkErrorType = NetworkErrorType.DELAY;
						else if (enteredLine.equals("2"))
							nthPacketNetworkErrorType = NetworkErrorType.DUPLICATE;
						else
							nthPacketNetworkErrorType = NetworkErrorType.LOST;
						break;
					}
					else{
						System.out.println("Select the right option from the following!");
						System.out.println("Please select one of the follwing options for the "+nthPacket+"th packet");
						System.out.println("1: DELAY");
						System.out.println("2: DUPLICATE");
						System.out.println("3: LOST");
					}
				}




				networkErrors.add(new NetworkErrorSpecs(nthPacket, nthPacketNetworkErrorType
						,nthPacketType));

				if(i < (numPackets -1 )){
					System.out.println("Will ask you the same set of questions for next packet/packets..");
					try {
						Thread.sleep(750);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}


			}

		}



		System.out.println("Please select one option from the following menu to simulate error");
		System.out.println("0: None. Don't Simulate the Error!");
		System.out.println("1: Missing FileName. Only Applies to the Request.");
		System.out.println("2: Missing Mode. Only Applies to the Request.");
		System.out.println("3: Invalid Opcode. Applies to both the Request as well as DATA and ACK.");
		System.out.println("4: Short ACK. ACK packet shorter than 4 bytes.");
		System.out.println("5: Error code 5. Unknown Transfer ID.");
		System.out.println("6: Long ACK. ACK packet longer than 4 bytes.");
		System.out.println("7: Long DATA. DATA packet longer than 516 bytes.");
		System.out.println("8: Short DATA. DATA packet shorter than 4 bytes.");
		//System.out.println("7: Short DATA. DATA packet shorter than 4 bytes.");
		while(true){
			enteredLine = userInput.nextLine();
			if(enteredLine.equals("0") || enteredLine.equals("1") || enteredLine.equals("2")||
					enteredLine.equals("3") || enteredLine.equals("4") || enteredLine.equals("5")||
					enteredLine.equals("6") || enteredLine.equals("7") || enteredLine.equals("8")){
				if(enteredLine.equals("1")){
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.MISSING_FILENAME, Packet.PacketType.REQ, -1);
					break;}
				else if(enteredLine.equals("2")){
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.MISSING_MODE, Packet.PacketType.REQ, -1);
					break;}
				else if(enteredLine.equals("3")){
					System.out.println("Please select one option from the following menu ");


					Packet.PacketType invalidPacketType;
					System.out.println("Please select one of the follwing options for the packet that you "
							+ "want to simulate Invalid Opcode on");
					System.out.println("1: ACK");
					System.out.println("2: DATA");
					System.out.println("3: REQUEST");
					while(true){
						enteredLine = userInput.nextLine();
						if(enteredLine.equals("1") || enteredLine.equals("2") || enteredLine.equals("3"))
						{
							if(enteredLine.equals("1"))
								invalidPacketType = Packet.PacketType.ACK;
							else if (enteredLine.equals("2"))
								invalidPacketType = Packet.PacketType.DATA;
							else
								invalidPacketType = Packet.PacketType.REQ;
							break;
						}
						else{
							System.out.println("Select the right option from the following!");
							System.out.println("Please select one of the follwing options for the packet that you "
									+ "want to simulate Invalid Opcode on");
							System.out.println("1: ACK");
							System.out.println("2: DATA");
							System.out.println("3: REQUEST");
						}

					}

					if(invalidPacketType != Packet.PacketType.REQ){
						boolean numberNotEntered;
						int invalidPacketBlockNumber = 0;
						numberNotEntered = true;
						while(numberNotEntered){
							System.out.println("Please enter the block number for the packet you want to simulate Invalid Opcode on");
							System.out.println("The number must be greater than 1");
							try {
								invalidPacketBlockNumber = Integer.parseInt(userInput.nextLine());
								if(invalidPacketBlockNumber>1)
									numberNotEntered = false;
							} catch( NumberFormatException e){}
						}
						simulatedError = new ErrorSimulateClass(Packet.ErrorCode.INVALID_OPCODE, invalidPacketType , invalidPacketBlockNumber);
					}
					else{
						simulatedError = new ErrorSimulateClass(Packet.ErrorCode.INVALID_OPCODE, Packet.PacketType.REQ, -1);	
					}
					break;
				}

				else if(enteredLine.equals("4")){
					boolean numberNotEntered;
					int shortAckBlockNumber = 0;
					numberNotEntered = true;
					while(numberNotEntered){
						System.out.println("Please enter the block number for the ACK packet "
								+ "you want to simulate shortACK on");
						System.out.println("The number must be greater than 1");
						try{
							shortAckBlockNumber = Integer.parseInt(userInput.nextLine());
							if(shortAckBlockNumber>1)
							numberNotEntered = false;
						}catch (NumberFormatException e){}
					}
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.SHORT_ACK, Packet.PacketType.ACK, shortAckBlockNumber);
					break;
				}

				else if(enteredLine.equals("5")){
					errorCode5[0] = true;

					System.out.println("Please select one of the follwing options for the packet that you "
							+ "want to simulate errorCode5 on");
					System.out.println("1: ACK");
					System.out.println("2: DATA");


					while(true){
						enteredLine = userInput.nextLine();
						if(enteredLine.equals("1") || enteredLine.equals("2") )
						{
							if(enteredLine.equals("1"))
								errorCode5[1] = false;
							else 
								errorCode5[1] = true;

							break;
						}
						else{
							System.out.println("Select the right option from the following!");
							System.out.println("Please select one of the follwing options for the packet that you "
									+ "want to simulate errorCode5 on");
							System.out.println("1: ACK");
							System.out.println("2: DATA");

						}

					}

					boolean numberNotEntered = true;
					while(numberNotEntered){
						System.out.println("Please enter the block number of the packet that you want to put error code  5 on");
						System.out.println("The number must be greater than 1");
						try {
							errorCode5BlockNumber = Integer.parseInt(userInput.nextLine());
							if(errorCode5BlockNumber>1)
								numberNotEntered = false;
						} catch( NumberFormatException e){}
					}
					
					
					if(errorCode5[1]){
						simulatedError = new ErrorSimulateClass(Packet.ErrorCode.UNKNOWN_TRANSFER_ID, Packet.PacketType.DATA, errorCode5BlockNumber );
					}
						
						
					else{
						simulatedError = new ErrorSimulateClass(Packet.ErrorCode.UNKNOWN_TRANSFER_ID, Packet.PacketType.ACK, errorCode5BlockNumber );
					}
					

					break;
				}

				else if(enteredLine.equals("6")){
					boolean numberNotEntered;
					int longACKBlockNumber = 0;
					numberNotEntered = true;
					while(numberNotEntered){
						System.out.println("Please enter the block number for the ACK packet "
								+ "you want to simulate LongACK on");
						System.out.println("The number must be greater than 1");
						try{
							longACKBlockNumber = Integer.parseInt(userInput.nextLine());
							if(longACKBlockNumber>1)
							numberNotEntered = false;
						}catch (NumberFormatException e){}
					}
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.LONG_ACK, Packet.PacketType.ACK, longACKBlockNumber);
					break;
				}
				
				else if(enteredLine.equals("7")){
					boolean numberNotEntered;
					int longDATABlockNumber = 0;
					numberNotEntered = true;
					while(numberNotEntered){
						System.out.println("Please enter the block number for the Data packet "
								+ "you want to simulate LongData on");
						System.out.println("The number must be greater than 1");
						try{
							longDATABlockNumber = Integer.parseInt(userInput.nextLine());
							if(longDATABlockNumber>1)
							numberNotEntered = false;
						}catch (NumberFormatException e){}
					}
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.LONG_DATA, Packet.PacketType.DATA, longDATABlockNumber);
					break;
				}


				else if(enteredLine.equals("8")){
					boolean numberNotEntered;
					int shortDATABlockNumber = 0;
					numberNotEntered = true;
					while(numberNotEntered){
						System.out.println("Please enter the block number for the Data packet "
								+ "you want to simulate ShortData on");
						System.out.println("The number must be greater than 1");
						try{
							shortDATABlockNumber = Integer.parseInt(userInput.nextLine());
							if(shortDATABlockNumber>1)
							numberNotEntered = false;
						}catch (NumberFormatException e){}
					}
					simulatedError = new ErrorSimulateClass(Packet.ErrorCode.SHORT_DATA, Packet.PacketType.DATA, shortDATABlockNumber);
					break;
				}

				else
					break;

			}else{
				System.out.println("Select the right option from the following!");
				System.out.println("0: None. Don't Simulate the Error!");
				System.out.println("1: Missing FileName. Only Applies to the Request.");
				System.out.println("2: Missing Mode. Only Applies to the Request.");
				System.out.println("3: Invalid Opcode. Applies to both the Request as well as DATA and ACK.");
				System.out.println("4: Short ACK. ACK packet shorter than 4 bytes.");
				System.out.println("5: Error code 5. Unknown Transfer ID.");
				System.out.println("6: Long ACK. ACK packet longer than 4 bytes.");
				System.out.println("7: Long DATA. DATA packet longer than 516 bytes.");
				System.out.println("8: Short DATA. DATA packet shorter than 4 bytes.");
				//System.out.println("7: Short DATA. DATA packet shorter than 4 bytes.");
			}

		}



		//userInput.close();
		return simulatedError;


	}






	public static void main(String args[])
	{

		ErrorSimulator imh = new ErrorSimulator();



		imh.sendAndReceive();

		/*ArrayList<NetworkErrorSpecs> networkErrors = new ArrayList<NetworkErrorSpecs>();
		Boolean[] quite = new Boolean[1];
		ErrorSimulateClass simulatedError;
		Boolean[] errorCode5 = new Boolean[2];
		errorCode5[0] = false; // initially its false.. unless set true otherwise

		simulatedError = imh.takeInitialUserInputAndGetErrorTYPE(networkErrors, quite, errorCode5);
		//System.out.println("Error Simulator has shut down");

		System.out.println("The packets that you have selected are as follows:");
		for( int i =0 ; i< networkErrors.size() ; i++){
			System.out.print(networkErrors.get(i).blockNumber+" ");
			System.out.print(networkErrors.get(i).packetType+" ");
			System.out.print(networkErrors.get(i).errorType);
			System.out.println("");
		}

		System.out.println(quite[0]);
		System.out.println(errorCode5[0]);
		System.out.println(errorCode5[1]);

		if(simulatedError != null)
		System.out.println(simulatedError.getDescription());
		 */
		System.out.println("Error Simulator has shut down");

		//imh.testErrorSimulate();

	}

}













