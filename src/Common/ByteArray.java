package Common;
import java.util.Arrays;

public class ByteArray {

	public static byte[] makePacketByte(String fileName, String mode){
		byte fileNameEncoded[] = fileName.getBytes();
		byte modeEncoded[] = mode.getBytes();
		byte ZeroByte[] = new byte[]{0}; 
		byte[] combined = new byte[fileNameEncoded.length + ZeroByte.length + modeEncoded.length + ZeroByte.length];

		System.arraycopy(fileNameEncoded,0, combined, 0, fileNameEncoded.length);
		System.arraycopy(ZeroByte, 0, combined, fileNameEncoded.length, ZeroByte.length);
		System.arraycopy(modeEncoded, 0, combined,fileNameEncoded.length + ZeroByte.length, modeEncoded.length);
		System.arraycopy(ZeroByte, 0, combined, fileNameEncoded.length + ZeroByte.length + modeEncoded.length, ZeroByte.length);

		return combined;

	}


	//used for assignment
	public static byte[] makeAcknowledgmentByte(boolean rrq){

		byte acknowledgement[];
		if(rrq){
			acknowledgement = new byte[] {0,3,0,1};
			System.out.println("It was a readRequest\n");
		}
		else{
			acknowledgement = new byte[] {0,4,0,0};
			System.out.println("It was a writeRequest\n");
		}

		return acknowledgement;
	}

	//used for project
	public static byte[] makeRealAcknowledgmentByte(int blockNumber){
		byte[] opcode = new byte[]{0,4};
		byte[] blkNumber = convertIntToByteArray(blockNumber);
		byte[] combined = new byte[opcode.length + blkNumber.length];
		System.arraycopy(opcode,0, combined, 0, opcode.length);
		System.arraycopy(blkNumber, 0, combined, opcode.length, blkNumber.length);
		return combined;	
	}

	public static byte[] makeDataByte(int blockNumber, byte[] data){
		byte[] opcode = new byte[]{0,3};
		byte[] blkNumber = convertIntToByteArray(blockNumber);
		byte[] combined = new byte[opcode.length + blkNumber.length + data.length];


		System.arraycopy(opcode,0, combined, 0, opcode.length);
		System.arraycopy(blkNumber, 0, combined, opcode.length, blkNumber.length);
		System.arraycopy(data, 0, combined,opcode.length + blkNumber.length, data.length);
		return combined;

	}

	public static byte[] makeErrorCodeByte(int errorCode, String errorMsg){
		if(errorCode<0 || errorCode>7)
			return null;

		byte[] opCode = new byte[]{0,5};
		byte[] errorCodeByteArray = new byte[]{0,(byte) errorCode};
		byte[] errorMsgByteArray = errorMsg.getBytes();
		byte[] combined = new byte[opCode.length+errorCodeByteArray.length+ errorMsgByteArray.length + 1];

		System.arraycopy(opCode, 0, combined, 0, opCode.length);
		System.arraycopy(errorCodeByteArray, 0, combined, opCode.length, errorCodeByteArray.length);
		System.arraycopy(errorMsgByteArray, 0, combined, opCode.length + errorCodeByteArray.length, errorMsgByteArray.length);
		combined[combined.length-1] = 0;

		
		return combined;
	}



	public static byte[] addReadRequestToPacketByte(byte msg[]){ 
		byte req[] = new byte[] {0, 1};
		byte[] combined = new byte[ req.length + msg.length];
		System.arraycopy(req,0, combined, 0, req.length);
		System.arraycopy(msg, 0, combined, req.length, msg.length);

		return combined;
	}


	public static byte[] addWriteRequestToPacketByte(byte msg[]){
		byte req[] = new byte[] {0, 2};
		byte[] combined = new byte[req.length + msg.length];


		System.arraycopy(req,0, combined, 0, req.length);
		System.arraycopy(msg, 0, combined, req.length, msg.length);

		return combined;

	}


	public static byte[] addInvalidRequestToPacketByte(byte msg[]){
		byte req[] = new byte[] {1,2};
		byte[] combined = new byte[msg.length + req.length];


		System.arraycopy(req,0, combined, 0, req.length);
		System.arraycopy(msg, 0, combined, req.length, msg.length);

		return combined;
	}


	/*
	    public static byte[] convertIntToByteArray(int intNumber){
			intNumber = intNumber-32768;
		 	byte byteNumber[] = new byte[2];
			byteNumber[0] = (byte) (intNumber & 0xFF);
			byteNumber[1] = (byte) ((intNumber >> 8) & 0xFF);
			return byteNumber;
		}




		public static int convertByteArrayToInt(byte[] byteNumber){
			return (((int)byteNumber[1] << 8) | ((int)byteNumber[0] & 0xFF)) + 32768;
		}
	 */




	public static int convertByteArrayToInt(byte[] byteNumber) {
		// return (byteNumber[0]<<8)|(byteNumber[1]&0xff);  //net byte order = Big-Endian
		int firstDigit = (int)byteNumber[0] & 0x000000FF;
		int secondDigit = (int)byteNumber[1] & 0x000000FF;

		int result = firstDigit * 256 + secondDigit;
		return result;
	}


	public static byte[] convertIntToByteArray(int intNumber) {
		byte byteNumber[] = new byte[2];
		byteNumber[0] = (byte) ((intNumber>>8)&0xff);
		byteNumber[1] = (byte) (intNumber&0xff);
		return byteNumber;
	}


	public static String getByteArrayAsString(byte[] Byte){
		return Arrays.toString(Byte);
	}


	public static void main(String[] args){
		byte[] check;
		check = convertIntToByteArray(65535);
		int check2 = convertByteArrayToInt(check);
		System.out.println(getByteArrayAsString(check));;
		System.out.println(check2);
		System.out.println(Arrays.toString(makeErrorCodeByte(0,"FileAlreadyExists")));

	}




}
