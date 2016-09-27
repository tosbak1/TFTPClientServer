import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


import Common.Packet;

public class ConnectionListener implements Runnable{


	private DatagramSocket socket69;
	private DatagramPacket receivePacket;
	private int activeConnectionsCount;
	private Boolean quite[];




	public ConnectionListener(Boolean quite[]){
		try {
			socket69 = new DatagramSocket(69);
		} catch (SocketException se) {   
			se.printStackTrace();
			System.exit(1);
		}
		activeConnectionsCount = 0;
		this.quite = quite;
	}



	public void closeSocket69(){
		socket69.close();
	}







	@Override
	public void run() {

		while(true){

			byte data[] = new byte[518];
			receivePacket = new DatagramPacket(data, data.length);


			try {
				socket69.receive(receivePacket);


			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Server has stopped listening on user command or "
						+ "is unable to listen anymore for any unexpected exeption. ");
				//e.printStackTrace();
			}

			if (socket69.isClosed())
				break; 


			System.out.println("A request has been received while listening on port 69.");
			if(!quite[0])
				Packet.displayReceivedPacket(receivePacket, "ConnectionListener");

			String connectionId = "connectionManager No "+String.valueOf(activeConnectionsCount);
			Thread connectionManagerThread = new Thread(new ConnectionManager(receivePacket, quite),connectionId);
			activeConnectionsCount++;
			connectionManagerThread.start();
			System.out.println("New connection manager has formed: "+connectionId);


		}


	}



}




