package client;

import java.io.IOException;
import java.net.UnknownHostException;

import de.fhac.mazenet.server.generated.LoginMessageType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;

public class ConnectionToServerTest {
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Baue Verbindung zu Server auf...");
		SSLConnection connection = new SSLConnection("127.0.0.1", 5432);
		System.out.println("Verbindung erfolgreich aufgebaut");
		MazeCom mazecom = new MazeCom();
		mazecom.setMcType(MazeComType.LOGIN);
		LoginMessageType login = new LoginMessageType();
		login.setName("DarkDev");
		mazecom.setLoginMessage(login);
		
		System.out.println("Sende LoginMessage zum Server....");
		connection.sendToServer(mazecom);
		System.out.println("LoginMessage wurde gesendet");
		MazeCom reply = connection.receiveFromServer();
		System.out.println("Antwort vom Server erhalten");
		if(reply == null)
		{
			System.out.println("Keine Antwort vom Server!");
		}
		System.out.println("Id von DarkDev ist " + reply.getLoginReplyMessage().getNewID());
	}
}
