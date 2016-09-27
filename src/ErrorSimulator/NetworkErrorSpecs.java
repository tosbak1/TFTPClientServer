package ErrorSimulator;

import Common.Packet;


public class NetworkErrorSpecs{
	public NetworkErrorType errorType;
	public Packet.PacketType packetType;
	public int blockNumber;
	public NetworkErrorSpecs(int blockNumber, NetworkErrorType errorType, Packet.PacketType packetType){
		this.blockNumber = blockNumber;
		this.errorType = errorType;
		this.packetType = packetType;
	}
}
