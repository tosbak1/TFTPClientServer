package Common;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public  class Packet {

	public enum PacketType{ACK, DATA, REQ, ERROR0, ERROR1,ERROR2,ERROR3, ERROR4, ERROR5, ERROR6, INVALID;}
	public enum ErrorCode{
		FILE_SIZE_NOT_SUPPORTED(0,"The file you are trying to read is larger than 65534 bytes."),
		FILENAME_IS_DIRECTORY(0,"File you are trying to read or write is a directory"),
		FILE_NOT_FOUND(1, "The file you are trying to read does not exist"),
		ACCESS_VIOLATION(2, "File access denied"),
		DISK_FULL(3, "The disk is full"),
		MISSING_FILENAME(4,"Missing FileName. The Request does not contain FileName."),
		MISSING_MODE(4,"Missing Mode. The Request does not contain mode."), 
		INVALID_OPCODE(4,"Invalid Opcode. The Opcode matches nothing in TFTP Specifications"),
		SHORT_ACK(4,"ACK packet shorter than 4 bytes."),
		LONG_ACK(4,"ACK packet longer than 4 bytes."),
		LONG_DATA(4,"DATA packet longer than 516 bytes."),
		SHORT_DATA(4,"DATA packet shorter than 4 bytes"),
		UNKNOWN_TRANSFER_ID(5,"Coming From UnExpected Port"),
		FILE_ALREADY_EXIST(6, "The file you are trying to write already exists");
		//NO_SUCH_USER(7, "The user does not exist");

		private int errorCode;
		private String description;
		private ErrorCode(int errorCode, String description){
			this.errorCode = errorCode;
			this.description = description;}

		public int getErrorCode(){
			return errorCode;
		}

		public String getDescription(){
			return description;
		}
	}


	public static  DatagramPacket makeReadRequest (String file, String mode, InetAddress address, int port ){
		byte[] msg =  ByteArray.addReadRequestToPacketByte(ByteArray.makePacketByte(file, mode));
		DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port );
		return packet;
	}
	public static DatagramPacket makeWriteRequest (String file, String mode, InetAddress address, int port ){
		byte[] msg =  ByteArray.addWriteRequestToPacketByte(ByteArray.makePacketByte(file, mode));
		DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port );
		return packet;
	}

	public static DatagramPacket makeDataBlock(int blockNumber, byte[] data, InetAddress address, int port){
		byte[] msg = ByteArray.makeDataByte(blockNumber, data);
		DatagramPacket packet = new DatagramPacket(msg,msg.length,address,port);
		return packet;
	}

	public static DatagramPacket makeErrorCodePacket(InetAddress address, int port ,int errorCode, String errorMsg){
		byte[] msg = ByteArray.makeErrorCodeByte(errorCode, errorMsg);
		DatagramPacket packet = new DatagramPacket(msg,msg.length,address, port );
		return packet;
	}

	public static DatagramPacket makeRealAcknowledgment(int blockNumber, InetAddress address, int port){
		byte[] msg = ByteArray.makeRealAcknowledgmentByte(blockNumber);
		DatagramPacket packet = new DatagramPacket(msg,msg.length,address,port);
		return packet;
	}


	public static DatagramPacket makeInvalidRequest(String file, String mode, InetAddress address, int port){
		byte[] msg = ByteArray.addInvalidRequestToPacketByte(ByteArray.makePacketByte(file, mode));
		DatagramPacket packet = new DatagramPacket(msg,msg.length,address, port);
		return packet;
	}



	public static boolean isRequestValid(DatagramPacket packet){     
		int length = packet.getLength();
		byte[] data = packet.getData();
		int countFileNameCharacters = 0;
		int countModeCharacters = 0;
		if   (!( ((int)data[0] == 0  && (int)data[1] == 1) || (  (int)data[0] == 0  && (int)data[1] == 2) ))
			return false;

		for (int i = 2 ; i < data.length ; i++){
			if  ( (int)data[i] != 0)
				countFileNameCharacters++;

			else
				break;
		}

		if  (!(countFileNameCharacters>0))
			return false;

		for (int i = (2 + countFileNameCharacters + 1) ; i < data.length ; i++ ){
			if ( (int)data[i] != 0)
				countModeCharacters++;

			else
				break;
		}

		if (!(countModeCharacters > 0))
			return false;

		if ( (2 + countFileNameCharacters + 1 + countModeCharacters + 1) != length)
			return false;

		return true;
	}


	//must be used after determining that the packet is a valid request
	public static boolean isReadRequest(DatagramPacket packet){
		byte[] data = packet.getData();

		if ( (int)data[1] == 1)
			return true;

		else
			return false;
	}


	public static void displayReceivedPacket(DatagramPacket receivedPacket, String name){

		byte[] data = receivedPacket.getData();	
		System.out.println(name+": Packet received:");
		System.out.println("From host: " + receivedPacket.getAddress());
		System.out.println("Host port: " + receivedPacket.getPort());
		int len = receivedPacket.getLength();
		System.out.println("Length: " + len);
		System.out.println("Type of block: "+getPacketTypeString(receivedPacket));
		if(Packet.isRequestValid(receivedPacket))
			System.out.println("Name of file: "+Packet.getFileNameFromARequest(receivedPacket));
		if(data[1] == 3 || data[1] == 4)
			System.out.println("Block Number: "+Packet.getBlockNumber(receivedPacket));
		if(isErrorPacket(receivedPacket))
			System.out.println("Error Message: "+extractErrorMsg(receivedPacket));
		
		System.out.println("Containing: ");
		System.out.print("  In a Byte Array From: [");

		for (int z = 0 ; z < len ; z++){
			int n = (int) data[z]; 
			if (z == (len-1))
				System.out.print(n);

			else
				System.out.print(n+", ");
		}
		System.out.println("]");
		System.out.println("");
	}



	public static void displaySendingPacket( DatagramPacket sendingPacket, String name){

		byte[] data = sendingPacket.getData();	
		System.out.println(name+": Sending packet:");
		System.out.println("To host: " + sendingPacket.getAddress());
		System.out.println("Destination host port: " + sendingPacket.getPort());
		int len = sendingPacket.getLength();
		System.out.println("Length: " + len);
		System.out.println("Containing: ");
		System.out.println("Type of block: "+getPacketTypeString(sendingPacket));
		if(Packet.isRequestValid(sendingPacket))
			System.out.println("Name of file: "+Packet.getFileNameFromARequest(sendingPacket));
		if(data[1] == 3 || data[1] == 4)
			System.out.println("Block Number: "+Packet.getBlockNumber(sendingPacket));
		if(isErrorPacket(sendingPacket))
			System.out.println("Error Message: "+extractErrorMsg(sendingPacket));


		System.out.print("  In a Byte Array From: [");

		for (int z = 0 ; z < len ; z++){
			int n = (int) data[z]; 
			if (z == (len-1))
				System.out.print(n);

			else
				System.out.print(n+", ");
		}
		System.out.println("]");
		System.out.println("");
	}

	public static String getFileNameFromARequest(DatagramPacket request){


		byte[] wholeByteArray = request.getData();
		byte[] fileNameByteArray;
		String fileName;

		if(!Packet.isRequestValid(request))
			return null;


		else{
			int i = 2;
			while(true){
				if ((int) wholeByteArray[i] == 0 )
					break;
				i++;
			}

			int fileNamelength = i - 2;
			fileNameByteArray = new byte[fileNamelength];

			System.arraycopy(wholeByteArray, 2, fileNameByteArray, 0, fileNamelength);
			fileName = new String(fileNameByteArray);

			return fileName;
		}
	}

	//only use on acknowlegdments or data blocks... dont use on REQUESTS!!!
	public static int getBlockNumber(DatagramPacket block){
		byte[] wholeByteArray = block.getData();
		byte[] blockNumberArray = new byte[2];

		if(wholeByteArray.length>=4){
			blockNumberArray[0] = wholeByteArray[2];
			blockNumberArray[1] = wholeByteArray[3];}

		else{ // wierd values, highest value
			blockNumberArray[0] = -1;
			blockNumberArray[1] = -1;
		}

		return ByteArray.convertByteArrayToInt(blockNumberArray);

	}


	public static boolean isAcknowledgment(DatagramPacket packet){

		if (packet.getLength() != 4)
			return false;


		byte[] data = packet.getData();

		if ( (int)data[0] != 0)
			return false;

		if ( (int)data[1] != 4)
			return false;

		return true;
	}


	public static boolean isDataBlock(DatagramPacket packet){

		if (packet.getLength() < 4 || packet.getLength()> 516)
			return false;

		byte[] data = packet.getData();

		if ( (int)data[0] != 0)
			return false;

		if ( (int)data[1] != 3)
			return false;

		return true;
	}



	public static boolean isErrorPacket(DatagramPacket packet){
		int length = packet.getLength();
		byte[] data = packet.getData();
		int countErrorMsgCharacters = 0;

		if ( (int)data[0] != 0){
			//System.out.println("0th byte");
			return false;}

		if ( (int)data[1] != 5){
			//System.out.println("1th byte");
			return false;
		}
		if ( (int)data[2] != 0){
			//System.out.println("2th byte");
			return false;}

		if ( (int)data[3] < 0 || (int)data[3] > 6 ){
			//System.out.println("errorRange");
			return false;}

		for (int i = 4 ; i < data.length ; i++){
			if  ( (int)data[i] != 0)
				countErrorMsgCharacters++;

			else
				break;
		}
		//System.out.println("Count ErrorMsgCharacters"+countErrorMsgCharacters );

		if ( (4 + countErrorMsgCharacters  + 1 != length)){
			//System.out.println("what was the length"+length);
			return false;}

		return true;
	}




	public static boolean isErrorPacket0(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 0)
			return true;
		else
			return false;
	}


	public static boolean isErrorPacket1(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 1)
			return true;
		else
			return false;
	}

	public static boolean isErrorPacket2(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 2)
			return true;
		else
			return false;
	}

	public static boolean isErrorPacket3(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 3)
			return true;
		else
			return false;
	}

	public static boolean isErrorPacket4(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 4)
			return true;
		else
			return false;
	}

	public static boolean isErrorPacket5(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 5)
			return true;
		else
			return false;
	}


	public static boolean isErrorPacket6(DatagramPacket packet){

		byte[] data = packet.getData();

		if ( (int)data[3]  == 6)
			return true;
		else
			return false;
	}




	public static String getPacketTypeString(DatagramPacket packet){

		if(isRequestValid(packet)){
			if(isReadRequest(packet))
				return "RRQ";
			else
				return "WRQ";
		}


		if (isAcknowledgment(packet))
			return "ACK";

		else  if(isDataBlock(packet))
			return "DATA";

		else if (isErrorPacket(packet)){


			if(isErrorPacket0(packet))
				return "ERROR0";

			else if(isErrorPacket1(packet))
				return "ERROR1";

			else if(isErrorPacket2(packet))
				return "ERROR2";

			else if(isErrorPacket3(packet))
				return "ERROR3";

			else if(isErrorPacket4(packet))
				return "ERROR4";

			else if(isErrorPacket5(packet))
				return "ERROR5";

			else
				return "ERROR6";
		}

		else
			return "INVALID";

	}
	
	public static boolean isInvalidPacket(DatagramPacket packet){
		if(getPacketType(packet) == PacketType.INVALID)
			return true;
		else
			return false;
	}
	
	
	
	//must only be invoked after determining its an invalid packet
	public static ErrorCode determineInvalidPacketType(DatagramPacket packet){
		if(!isInvalidPacket(packet))
			return null;
		
		byte[] data = packet.getData();
		int len = packet.getLength();
		
		
		if(len == 3){
			if(data[1] == 3)
				return Packet.ErrorCode.SHORT_DATA;
			
			if(data[1]==4)
				return Packet.ErrorCode.SHORT_ACK;			
		}
		
		if(len == 5 && data[1] == 4){
			return Packet.ErrorCode.LONG_ACK;
		}
		
		if(len >516 && data[1] ==3)
			return Packet.ErrorCode.LONG_DATA;
		
		if(data[1] == 1 || data[1] == 2){
			
			if(data[2] == 0)
				return Packet.ErrorCode.MISSING_FILENAME;  
			
			if(data[len-2] == 0)
				return Packet.ErrorCode.MISSING_MODE;
			
		}
		
		
		
		return Packet.ErrorCode.INVALID_OPCODE;
		
		
		
		
	}

	public static PacketType getPacketType(DatagramPacket packet){
		if(isRequestValid(packet)){
			return Packet.PacketType.REQ;
		}


		if (isAcknowledgment(packet))
			return Packet.PacketType.ACK;

		else  if(isDataBlock(packet))
			return Packet.PacketType.DATA;

		else if (isErrorPacket(packet)){

			if(isErrorPacket0(packet))
				return Packet.PacketType.ERROR0;

			else if(isErrorPacket1(packet))
				return Packet.PacketType.ERROR1;

			else if(isErrorPacket2(packet))
				return Packet.PacketType.ERROR2;

			else if(isErrorPacket3(packet))
				return Packet.PacketType.ERROR3;

			else if(isErrorPacket4(packet))
				return Packet.PacketType.ERROR4;

			else if(isErrorPacket5(packet))
				return Packet.PacketType.ERROR5;

			else
				return Packet.PacketType.ERROR6;
		}

		else
			return Packet.PacketType.INVALID;
	}
	
	public static String extractErrorMsg(DatagramPacket packet){
		if(!isErrorPacket(packet))
			return null;
		
		String msg = "";
		byte[] data = packet.getData();
		for(int i = 4; i<data.length ; i++){
			if(data[i] == 0){
				break;
			}
			else
			msg = msg + (char)data[i];
		}
		
		return msg;
		
	}






	public static void main(String[] args){


		byte byteCheck[] = {0,5,0,8,112,123,0};


		DatagramPacket packetCheck = null;
		try {
			packetCheck = new DatagramPacket(byteCheck, byteCheck.length, InetAddress.getLocalHost(), 69);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//packetCheck.setLength(8);


		DatagramPacket ack = null;
		try {
			ack = Packet.makeRealAcknowledgment(25, InetAddress.getLocalHost(), 69);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(Packet.getPacketType(packetCheck));
		System.out.println(Packet.isInvalidPacket(packetCheck));

		System.out.println(ack.getData().length);
	}













}
