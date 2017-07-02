package client;

import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.LoginMessageType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;
import de.fhac.mazenet.server.generated.MoveMessageType;

/*
 * Adapterklasse für die Kommunikation zwischen Server und KI
 */

public class Client {
	private static int id;
	
	private static MoveMessageType getNextMove(AwaitMoveMessageType awaitMoveMessage, int id) {
		KI ki = new KI(awaitMoveMessage, id);
		return ki.getMove();
	}
	
	public static void main(String[] args) {
		int port = 5432;
		String IP = "127.0.0.1";

		if (args.length > 0)
			IP = args[0];
		if (args.length > 1)
			port = Integer.parseInt(args[1]);

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
		login.setName("DarkDev");
		mazecom.setLoginMessage(login);

		System.out.println("Sende LoginMessage zum Server....");
		connection.sendToServer(mazecom);
		System.out.println("LoginMessage wurde gesendet");

		System.out.println("Empfange Antwort vom Server...");
		MazeCom reply = connection.receiveFromServer();
		System.out.println("Antwort vom Server erhalten");

		id = reply.getLoginReplyMessage().getNewID();
		System.out.println("Die ID dieses Clients ist: " + Client.id);
		
		while(true) {
			reply = connection.receiveFromServer();
			
			AwaitMoveMessageType awaitMoveMessage = reply.getAwaitMoveMessage();
			MoveMessageType moveMessageType = getNextMove(awaitMoveMessage, id);
			
			mazecom = new MazeCom();
			mazecom.setMcType(MazeComType.MOVE);
			mazecom.setMoveMessage(moveMessageType);
			connection.sendToServer(mazecom);
			
			reply = connection.receiveFromServer();
		}
	}
}
