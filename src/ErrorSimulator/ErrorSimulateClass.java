package ErrorSimulator;

import java.net.DatagramPacket;


import Common.Packet;

public class ErrorSimulateClass{
	private Packet.ErrorCode errorType;
	private Packet.PacketType packetType;
	private int blockNumber;


	public synchronized Packet.ErrorCode getErrorType() {
		return errorType;
	}
	public synchronized Packet.PacketType getPacketType() {
		return packetType;
	}
	public synchronized int getBlockNumber() {
		return blockNumber;
	}
	public ErrorSimulateClass(Packet.ErrorCode errorType, Packet.PacketType packetType, int blockNumber){
		this(errorType);
		if( !(errorType == Packet.ErrorCode.MISSING_FILENAME || errorType == Packet.ErrorCode.MISSING_MODE) ){
			this.packetType = packetType;			
			this.blockNumber = blockNumber;
			this.errorType = errorType;}
	} 
	//This constructor will only be called  if the error is meant to be simulated on Requests
	private ErrorSimulateClass(Packet.ErrorCode errorType){
		this.errorType = errorType;
		packetType = Packet.PacketType.REQ;
		blockNumber = -1;
	}

	public String getDescription(){
		return (errorType+" "+packetType+" "+blockNumber);
	}




	public synchronized DatagramPacket simulateError(DatagramPacket packet){
		if (errorType == Packet.ErrorCode.MISSING_FILENAME && Packet.isRequestValid(packet)){
			//System.out.println("it goes through fileName");
			byte[] data = packet.getData();
			int length = packet.getLength();
			byte newPacketTemperoryByte[] = new byte[length];
			newPacketTemperoryByte[0] = data[0];
			newPacketTemperoryByte[1] = data[1];
			int i;
			for (i =2; i< length; i++){
				if (data[i] == 0){
					System.arraycopy(data, i, newPacketTemperoryByte, 2, (length - i));		           
					break;
				}
			}
			byte newPacketByte[] = new byte[length - (i-2)];
			System.arraycopy(newPacketTemperoryByte, 0, newPacketByte, 0, length - (i-2));
			packet.setLength(newPacketByte.length);
			packet.setData(newPacketByte);
			return packet;
		}


		if(errorType == Packet.ErrorCode.MISSING_MODE && Packet.isRequestValid(packet) ){
			//System.out.println("it goes through mode");
			byte[] data = packet.getData();
			int length = packet.getLength();
			byte newPacketTemperoryByte[] = new byte[length];
			newPacketTemperoryByte[0] = data[0];
			newPacketTemperoryByte[1] = data[1];
			int i;
			for(i=2; i<length;i++){
				if(data[i] == 0){
					System.arraycopy(data, 2, newPacketTemperoryByte, 2, i-1);
					break;
				}
			}

			byte newPacketByte[] = new byte[i+2];
			System.arraycopy(newPacketTemperoryByte, 0, newPacketByte, 0, i+2);
			packet.setLength(newPacketByte.length);
			packet.setData(newPacketByte);
			return packet;
		}

		if(errorType == Packet.ErrorCode.INVALID_OPCODE && Packet.isRequestValid(packet)){
			byte[] data = packet.getData();
			int length = packet.getLength();
			data[0] = 1;
			packet.setData(data);
			packet.setLength(length);
			return packet;
		}

		//this is where i will add code for error5 in future



		// m not putting here.. the messanger-- Errorcode5Simulator will deal with it


		//this is where i will end

		if(Packet.getBlockNumber(packet) == blockNumber && Packet.getPacketType(packet) == (packetType) &&
				errorType != Packet.ErrorCode.MISSING_MODE && errorType != Packet.ErrorCode.MISSING_FILENAME){
			if(errorType == Packet.ErrorCode.INVALID_OPCODE){
				byte[] data = packet.getData();
				int length = packet.getLength();
				data[0] = 1;
				packet.setData(data);
				packet.setLength(length);
				return packet;
			}

			if( (errorType == Packet.ErrorCode.SHORT_ACK && Packet.getPacketType(packet) == Packet.PacketType.ACK) 
					|| (errorType == Packet.ErrorCode.SHORT_DATA  && Packet.getPacketType(packet) ==Packet.PacketType.DATA)){
				byte[] data = packet.getData();
				byte[] newData = new byte[3];
				System.arraycopy(data, 0, newData, 0, 3);
				packet.setLength(newData.length);
				packet.setData(newData);
				return packet;
			}

			if(errorType == Packet.ErrorCode.LONG_ACK && Packet.getPacketType(packet) == Packet.PacketType.ACK){
				byte[] data = packet.getData();
				byte[] newData = new byte[5];
				byte last = 75;

				/*System.out.println("The length of data at a packet level is "+packet.getLength());
				System.out.println("The contents of data at a packet level is "+Arrays.toString(packet.getData()));
				System.out.println("The length of data at a byte level is "+data.length);
				System.out.println("The contents of data at a byte level are "+Arrays.toString(data));
				System.out.println("The length of new data at a byte level is "+newData.length);
				System.out.println("The contents at a byte level are "+Arrays.toString(newData));*/
				System.arraycopy(data, 0, newData, 0, packet.getLength());
				newData[4] = last;
				//System.out.println(Arrays.toString(newData));
				packet.setLength(newData.length);
				packet.setData(newData);
				return packet;
			}

			if(errorType == Packet.ErrorCode.LONG_DATA && Packet.getPacketType(packet) ==Packet.PacketType.DATA){
				byte[] data = packet.getData();
				byte[] newData = new byte[517];
				byte last = 44;
				System.arraycopy(data, 0, newData, 0, packet.getLength());
				newData[516] = last;
				packet.setLength(newData.length);
				packet.setData(newData);
				return packet;
			}


		}

		return packet;
	}
}
