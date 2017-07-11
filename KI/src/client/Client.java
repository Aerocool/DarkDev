package client;

import java.util.concurrent.ThreadLocalRandom;

import KI.ArtificialIntelligence;
import KI.KI;
import KI.KIThilo;
import de.fhac.mazenet.server.generated.AcceptMessageType;
import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.LoginMessageType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;
import de.fhac.mazenet.server.generated.MoveMessageType;

/*
 * Adapterklasse für die Kommunikation zwischen Server und KI
 */

public class Client {
	private String IP;
	private int port;
	private String playerName;
	private boolean simulateFurtherTurns = true;
	
	public Client(String IP, int port, String playerName) {
		this.playerName = playerName;
		this.IP = IP;
		this.port = port;
	}
	
	private void setSimulateFurtherTurns(boolean simulation) {
		this.simulateFurtherTurns = simulation;
	}
	
	public String getPlayerName() {
		return playerName;
	}
	
	public void connectToServer() {
		SSLConnection connection = null;
		System.out.println("Baue Verbindung zu Server auf...");
		try {
			connection = new SSLConnection(IP, port);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Verbindungsaufbau ist fehlgeschlagen.");
			System.exit(1);
		}
		System.out.println("Verbindung erfolgreich aufgebaut");

		// Objekt, über das mit dem Server kommuniziert wird
		MazeCom mazecom = new MazeCom();
		mazecom.setMcType(MazeComType.LOGIN);
		LoginMessageType login = new LoginMessageType();
		login.setName(playerName);
		mazecom.setLoginMessage(login);

		System.out.println("Sende LoginMessage zum Server....");
		connection.sendToServer(mazecom);
		System.out.println("LoginMessage wurde gesendet");

		System.out.println("Empfange Antwort vom Server...");
		MazeCom reply = connection.receiveFromServer();
		System.out.println("Antwort vom Server erhalten");

		int playerId = reply.getLoginReplyMessage().getNewID();
		KIThilo aiTmp = new KIThilo(playerId);
		aiTmp.setSimulateFurtherTurns(this.simulateFurtherTurns);
		ArtificialIntelligence AI = aiTmp;
		System.out.println("Die ID dieses Clients ist: " + playerId);
		
		while(true) {
			reply = connection.receiveFromServer();
			
			if(reply.getWinMessage() != null) {
				break;
			}
			if(reply.getDisconnectMessage() != null)
				break;
			AwaitMoveMessageType awaitMoveMessage = reply.getAwaitMoveMessage();
			MoveMessageType moveMessageType = null;
			try {
				moveMessageType = AI.getNextMove(awaitMoveMessage);
			} catch(Exception e) {
				AI = new KI(playerId);
				moveMessageType = AI.getNextMove(awaitMoveMessage);
			}
			
			if(AI instanceof KI) {
				KIThilo ai = new KIThilo(playerId);
				ai.setSimulateFurtherTurns(this.simulateFurtherTurns);
				AI = ai;
			}
			
			mazecom = new MazeCom();
			mazecom.setMcType(MazeComType.MOVE);
			mazecom.setMoveMessage(moveMessageType);
			connection.sendToServer(mazecom);
			
			reply = connection.receiveFromServer();
			AcceptMessageType acceptMessage = reply.getAcceptMessage();
			if(acceptMessage.isAccept() == false) {
				AI = new KI(playerId);
			} 
		}
		System.out.println("GG");
		System.out.println("Trenne Verbindung zum Server...");
		connection.closeConnection();
	}

	public static void main(String[] args) {
		int port = 5432;
		String IP = "127.0.0.1";

		if (args.length > 0)
			IP = args[0];
		if (args.length > 1)
		try {
			port = Integer.parseInt(args[1]);
		} catch(Exception e) {
			System.out.println("Der angebene Port \"" + args[1] + "\" ist kein gültiger Wert. Stelle auf Standardport " + port);
		}
		System.out.println("Eingebene IP ist: " + IP);
		
		Client client = new Client(IP, port, "DarkDevGood");
		client.setSimulateFurtherTurns(true);
		client.connectToServer();
		
//		Client client2 = new Client(IP, port, "DarkDev");
//		client2.setSimulateFurtherTurns(false);
//		Client client3 = new Client(IP, port, "DarkDev");
//		client3.setSimulateFurtherTurns(false);
//		Client client4 = new Client(IP, port, "DarkDev");
//		client4.setSimulateFurtherTurns(false);
//		
//		Thread th = new Thread(() -> { client.connectToServer();});
//		Thread th2 = new Thread(() -> { client2.connectToServer();});
//		Thread th3 = new Thread(() -> { client3.connectToServer();});
//		Thread th4 = new Thread(() -> { client4.connectToServer();});
//		
//		Thread[] threads = {th, th2, th3, th4};
//		while(th.isAlive() == false || th2.isAlive() == false || th3.isAlive() == false || th4.isAlive() == false) {
//			int number = ThreadLocalRandom.current().nextInt(0, 3 + 1);
//			if(threads[number].isAlive() == false)
//				threads[number].start();
//		}
	}
}
