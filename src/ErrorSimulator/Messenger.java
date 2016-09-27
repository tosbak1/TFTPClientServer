package ErrorSimulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import Common.Packet;



public class Messenger implements Runnable{

	private DatagramSocket receivingSocket, sendingSocket;
	private InetAddress destinationAddress;
	private int destinationPort;
	private DatagramPacket movingPacket;
	private  boolean myJobDone;
	private ArrayList<NetworkErrorSpecs> networkErrors;
	private boolean firstTime;
	private volatile Integer threadEndCount[];
	private Boolean quite[];
	private ErrorSimulateClass errorSimulate;
	boolean errorCode5Simulated;


	//used for simulation of regular error
	//private Packet.ErrorCode errorType;
	private Packet.PacketType packetType;
	private int blockNumber;



	public  Messenger(DatagramSocket receivingSocket, DatagramSocket sendingSocket, 
			InetAddress destinationAddress, int destinationPort, 
			ArrayList<NetworkErrorSpecs> networkErrors, Integer threadEndCount[], Boolean quite[], ErrorSimulateClass errorSimulate){
		this.receivingSocket = receivingSocket;
		this.sendingSocket = sendingSocket;
		this.destinationPort = destinationPort;
		this.destinationAddress = destinationAddress;
		this.networkErrors = networkErrors;
		myJobDone = false;
		this.threadEndCount = threadEndCount;
		this.quite = quite;
		this.errorSimulate = errorSimulate;
		errorCode5Simulated = false;


		if(errorSimulate!= null){
			//this.errorType = errorSimulate.getErrorType();
			this.packetType = errorSimulate.getPacketType();
			this.blockNumber = errorSimulate.getBlockNumber();
		}
		else{
			//this.errorType = null;
			this.packetType = null;
			this.blockNumber = -1;
		}


	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		firstTime = true;
		while(!myJobDone /*&& !lastAckBlockSent*/){
			try {
				ReceiveAndForward();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadEndCount[0]++;
		System.out.println(toString()+": DONE");
	}

	private void ReceiveAndForward() throws IOException{    
		//byte data[] = new byte[516];
		byte data[] = new byte[518];
		movingPacket = new DatagramPacket(data, data.length);

		if(!firstTime){ 
			try {
				receivingSocket.setSoTimeout(8000);
				//System.out.println("time set successful");
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}}

		try {
			receivingSocket.receive(movingPacket);
		} catch (SocketTimeoutException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println(toString() +": Waiting. Nothing received! timed out");
			myJobDone = true;
			return;
		}

		synchronized(quite){
			if(!quite[0]){
				Packet.displayReceivedPacket(movingPacket, toString());
				System.out.println("");}}



		forward();
		firstTime = false;

	}





	private void forward(){
		int messUpWithThisPacket =  getIndex(movingPacket);

		/*
		for(int i = 0 ; i < networkErrors.size() ;i++){
		if((Packet.getBlockNumber(movingPacket) == networkErrors.get(i).blockNumber &&
			Packet.isDataBlock(movingPacket) && networkErrors.get(i).packetType == PacketType.DATA)
				||
			(Packet.getBlockNumber(movingPacket) == networkErrors.get(i).blockNumber &&
		Packet.isAcknowledgment(movingPacket) && networkErrors.get(i).packetType == PacketType.ACK))
			messUpWithThisPacket = i;
			break;
		}*/

		//System.out.println("messedup value is "+ messUpWithThisPacket);



		//we don't have to mess up with this packet
		if(messUpWithThisPacket == -1){  








			if(errorSimulate!=null){
				if(errorSimulate.getErrorType() == Packet.ErrorCode.UNKNOWN_TRANSFER_ID && 
						blockNumber == Packet.getBlockNumber(movingPacket) &&
						packetType == Packet.getPacketType(movingPacket)
						&& !errorCode5Simulated){
					sendReceivePacketFromUnknownHost();
					errorCode5Simulated = true;
				}

				else{
					movingPacket.setPort(destinationPort);
					movingPacket.setAddress(destinationAddress);
					if(blockNumber== Packet.getBlockNumber(movingPacket)){
						if(packetType == Packet.getPacketType(movingPacket)){
							//System.out.println("Messenger "+toString()+": length of data at a byte level "+movingPacket.getData().length);
							errorSimulate.simulateError(movingPacket);
						}

					}	
				}
			}
			movingPacket.setPort(destinationPort);
			movingPacket.setAddress(destinationAddress);



			try {
				sendingSocket.send(movingPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			synchronized(quite){
				if(!quite[0]){
					Packet.displaySendingPacket(movingPacket, toString());
					System.out.println("");}}





			/*
			if(Packet.isDataBlock(movingPacket) && movingPacket.getLength()<516){
				lastDataBlockSent = true;
				myJobDone = true;
				lastDataBlockSentNumber = Packet.getBlockNumber(movingPacket);
			}
			if(Packet.isAcknowledgment(movingPacket) && lastDataBlockSent && (Packet.getBlockNumber(movingPacket) == lastDataBlockSentNumber)){
				lastAckBlockSent = true;
				myJobDone = true;
			} */
		}



		else{

			if(networkErrors.get(messUpWithThisPacket).errorType == NetworkErrorType.LOST){
				System.out.println("\n \n \n"+Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+": I am not sending.. This packet will be lost");
				removeFromNetworkErrors(messUpWithThisPacket);
				System.out.println(Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+" Lost Packet Succeed  \n \n \n");
			}


			else if(networkErrors.get(messUpWithThisPacket).errorType == NetworkErrorType.DELAY){
				System.out.println("\n \n \n"+Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+": Will wait for 4 seconds.. And than send");
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(errorSimulate!=null){
					if(errorSimulate.getErrorType() == Packet.ErrorCode.UNKNOWN_TRANSFER_ID && 
							blockNumber == Packet.getBlockNumber(movingPacket) &&
							packetType == Packet.getPacketType(movingPacket)
							&& !errorCode5Simulated){
						sendReceivePacketFromUnknownHost();
						errorCode5Simulated = true;
					}

					else{
						movingPacket.setPort(destinationPort);
						movingPacket.setAddress(destinationAddress);
						if(blockNumber== Packet.getBlockNumber(movingPacket)){
							if(packetType == Packet.getPacketType(movingPacket)){
								//System.out.println("Messenger "+toString()+": length of data at a byte level "+movingPacket.getData().length);
								errorSimulate.simulateError(movingPacket);
							}

						}	
					}
				}
				movingPacket.setPort(destinationPort);
				movingPacket.setAddress(destinationAddress);


				try {
					sendingSocket.send(movingPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				synchronized(quite){
					if(!quite[0]){
						Packet.displaySendingPacket(movingPacket, toString());
						System.out.println("");}}
				removeFromNetworkErrors(messUpWithThisPacket);
				System.out.println(Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+" Delayed Packet Succeed  \n \n \n");
			}


			else{ //DUPLICATE
				System.out.println("\n \n \n"+Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+": Sending this Packet twice with 1 second interval");




				if(errorSimulate!=null){
					if(errorSimulate.getErrorType() == Packet.ErrorCode.UNKNOWN_TRANSFER_ID && 
							blockNumber == Packet.getBlockNumber(movingPacket) &&
							packetType == Packet.getPacketType(movingPacket)
							&& !errorCode5Simulated){
						sendReceivePacketFromUnknownHost();
						errorCode5Simulated = true;
					}

					else{
						movingPacket.setPort(destinationPort);
						movingPacket.setAddress(destinationAddress);
						if(blockNumber== Packet.getBlockNumber(movingPacket)){
							if(packetType == Packet.getPacketType(movingPacket)){
								//System.out.println("Messenger "+toString()+": length of data at a byte level "+movingPacket.getData().length);
								errorSimulate.simulateError(movingPacket);
							}

						}	
					}
				}
				movingPacket.setPort(destinationPort);
				movingPacket.setAddress(destinationAddress);

				try {
					sendingSocket.send(movingPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				synchronized(quite){
					if(!quite[0]){
						Packet.displaySendingPacket(movingPacket, toString());
						System.out.println("");}}




				if(errorSimulate!=null){
					if(errorSimulate.getErrorType() == Packet.ErrorCode.UNKNOWN_TRANSFER_ID && 
							blockNumber == Packet.getBlockNumber(movingPacket) &&
							packetType == Packet.getPacketType(movingPacket)
							&& !errorCode5Simulated){
						sendReceivePacketFromUnknownHost();
						errorCode5Simulated = true;
					}

					else{
						movingPacket.setPort(destinationPort);
						movingPacket.setAddress(destinationAddress);
						if(blockNumber== Packet.getBlockNumber(movingPacket)){
							if(packetType == Packet.getPacketType(movingPacket)){
								//System.out.println("Messenger "+toString()+": length of data at a byte level "+movingPacket.getData().length);
								errorSimulate.simulateError(movingPacket);
							}

						}	
					}
				}
				movingPacket.setPort(destinationPort);
				movingPacket.setAddress(destinationAddress);


				try {
					sendingSocket.send(movingPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				synchronized(quite){
					if(!quite[0]){
						Packet.displaySendingPacket(movingPacket, toString());
						System.out.println("");}}
				removeFromNetworkErrors(messUpWithThisPacket);
				System.out.println(Packet.getBlockNumber(movingPacket)+" "+ Packet.getPacketTypeString(movingPacket)+" Duplicate Packet Succeed  \n \n \n");
			}

		}



	}


	public void sendReceivePacketFromUnknownHost(){
		byte[] data = new byte[518];
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		DatagramSocket sendReceiveSocket = null;

		movingPacket.setPort(destinationPort);
		movingPacket.setAddress(destinationAddress);

		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			sendReceiveSocket.send(movingPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized(quite){
			if(!quite[0]){
				Packet.displaySendingPacket(movingPacket, toString()+", UnknownHost");
				//System.out.println("sent from a newly developed socket to simulate errorCode5");
				System.out.println("");}}

		try {
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		synchronized(quite){
			if(!quite[0]){
				Packet.displayReceivedPacket(receivePacket, toString()+", UnknownHost");
				//System.out.println("received from a newly developed socket to simulate errorCode5");
				System.out.println("");}}


	}









	public String toString(){
		return Thread.currentThread().getName();
	}

	public void removeFromNetworkErrors(int messedUpwithThis){
		synchronized(networkErrors){
			networkErrors.remove(messedUpwithThis);
		}
	}


	public int getIndex(DatagramPacket movingPacket){
		int index = -1;
		synchronized(networkErrors){



			for(int i = 0 ; i < networkErrors.size() ;i++){
				if((Packet.getBlockNumber(movingPacket) == networkErrors.get(i).blockNumber &&
						Packet.isDataBlock(movingPacket) && networkErrors.get(i).packetType == Packet.PacketType.DATA)
						||
						(Packet.getBlockNumber(movingPacket) == networkErrors.get(i).blockNumber &&
						Packet.isAcknowledgment(movingPacket) && networkErrors.get(i).packetType == Packet.PacketType.ACK))
					index = i;
				break;
			}
		}
		//System.out.println("I am returning this as an index"+index);
		return index;
	}




}
