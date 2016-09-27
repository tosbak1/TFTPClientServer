import java.util.Scanner;



public class Server {
	
	private ConnectionListener connectionListener;
	private Thread connectionListenerThread;
	private Scanner reader2;
	private Boolean quite[];
	
	
	public Server(Boolean quite[]){
		this.quite = quite;
		connectionListener = new ConnectionListener(this.quite);
		connectionListenerThread = new Thread(connectionListener);
		reader2 = new Scanner(System.in);

	}
	
	public void startup(){
			connectionListenerThread.start();
			while(true){
				listenForQuitCommand();
				shutDown();
				break;
			}
	}
	
	public void shutDown(){
		connectionListener.closeSocket69();
	}
	
	public void listenForQuitCommand(){
		System.out.println("Type quit to shut the server off, you can enter quit anytime to shut down the server");
		while(true){
		String readLine = reader2.nextLine();
		if(readLine.equals("quit")){
			reader2.close();
			break;
				}
		System.out.println("Type 'quit' in lowercase!");
		}
	}
	
	
	
	public static void main(String[] args){
		Scanner reader;
		reader = new Scanner(System.in);
		
		String userInput;
		Boolean quite[] = new Boolean[1];
		
		  //is it a quite mode or verbose mode
		  System.out.println("Please enter 1 for quite and 2 for verbose mode.");
		  while(true){
			  userInput = reader.nextLine();
			  
			  if(userInput.equals("1") || userInput.equals("2"))
			  	{
				  if(userInput.equals("1"))
					  quite[0] = true;
		  
				  else
					  quite[0] = false;
				  break;
		   		}
		   
			  else
				  System.out.println("Please enter 1 for quite and 2 for verbose mode!!!");
		  }
		  
		  
		
		Server s = new Server(quite);
		s.startup();
		reader.close();
	}
}
