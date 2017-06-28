package client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import de.fhaachen.mazenet.generated.AwaitMoveMessageType;
import de.fhaachen.mazenet.generated.ErrorType;
import de.fhaachen.mazenet.generated.LoginReplyMessageType;
import de.fhaachen.mazenet.generated.MazeCom;
import de.fhaachen.mazenet.generated.MazeComType;
import de.fhaachen.mazenet.generated.MoveMessageType;
import de.fhaachen.mazenet.generated.ObjectFactory;
import de.fhaachen.mazenet.networking.MazeComMessageFactory;
import de.fhaachen.mazenet.networking.XmlOutStream;

/**
 * 
 * 
 * Dies ist eine rudimentäre Client-Klasse - kann sich momentan nur einloggen.
 * Am besten ist es, den Networking-Teil (Nachrichtenaustausch mit Server)
 * genauso wie auf Server-Seite zu machen.
 * 
 * //TODO: LoginProzess gleich machen mit LoginThread wie auf Server-Seite
 * 
 * @author johannesw
 *
 */
public class TCPClient_JW {

	public static final String ACTORCHOICE_KI = "robot";
	public static final String ACTORCHOICE_HUMAN = "flesh";

	// for sending and and receiving from server
	private ServerListener_JW serverListener;
	private Socket socketToServer;
	private XmlOutStream outToServer;

	// for making messages in general
	private ObjectFactory oFactory;
	private MazeComMessageFactoryExtended mcFactory;

	// for making moves
	private String playerName;
	private String actorChoice;
	private Actor actor;
	private MazeCom moveMsg;
	private boolean hasMoveToSend;

	/**
	 * 
	 * @param args
	 *            - "local": server running on local machine (for testing) or
	 *            ip-address: server provided by tutors (for competition) - "ki"
	 *            or "flesh"
	 */
	public static void main(String[] args) {

		if ((args.length != 2) || (!args[1].equals(ACTORCHOICE_KI) && !args[1].equals(ACTORCHOICE_HUMAN))) {
			System.out.println("Input argument required:");
			System.out.println("\targ1: serverIP address, or 'local' if server runs on this machine).");
			System.out.println("\targ2: 'robot' for computer player, 'flesh' for user playing by cmd line.");
			return;
		}

		int hostPort = de.fhaachen.mazenet.config.Settings.PORT;
		String hostAddress;

		if (args[0].equals("local")) { // server runs locally

			/*
			 * spielt man mit mehr als einem spieler (Bsp.: 2), fuehrt das
			 * Ermitteln der hostAdress via InetAdress (siehe unten), bei mir
			 * (JW) dazu dass der Server nur den zuletzt angemeldeten Client
			 * zulaesst, und zu dem zuerst angemeldeten ausspuckt:
			 * "[Warnunng]: Host 192.168.56.1 ist bereits vorhanden" (entspricht
			 * dem obigen Wert von hostAdress bei JW). Versuche deshalb mal
			 * stattdessen fuer 'local' 127.0.0.1 zu setzen.
			 */
			hostAddress = "127.0.0.1";

			// try {
			// hostAddress = InetAddress.getLocalHost().getHostName();
			/*
			 * } catch (UnknownHostException e) { System.out.println(
			 * "Unable to determine localhost address."); e.printStackTrace();
			 * return; }
			 */
		} else { // server runs on remote machine
			hostAddress = args[0];
		}
		String actorChoice = args[1];

		try {
			TCPClient_JW client = new TCPClient_JW(hostAddress, hostPort, actorChoice);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public TCPClient_JW(String hostAddress, int hostPort, String actorChoice) throws UnknownHostException, IOException {

		oFactory = new ObjectFactory();
		mcFactory = new MazeComMessageFactoryExtended();
		this.actorChoice = actorChoice;

		if (!login(hostAddress, hostPort)) {

			System.out.println("client: constr: login true, entering playGame.");
			playGame();

		}

	}

	public boolean login(String hostAddress, int hostPort) {

		boolean loginFailed = true;

		try {
			// establish connection, start listening on serversocket
			this.socketToServer = new Socket(hostAddress, hostPort);
			this.outToServer = new XmlOutStream(socketToServer.getOutputStream());
			this.serverListener = new ServerListener_JW(this, this.socketToServer);
			this.serverListener.start();
			System.out.println("Client is online and listening to server.\nTrying login...");

			// init variables for login loop
			MazeComMessageFactory fac = new MazeComMessageFactory();
			Random randSeed = new Random();
			playerName = "";

			/*
			 * enter 'try to login' - loop.
			 */
			while (serverListener.isAlive() && serverListener.getAnswerToLoginMessage() == ErrorType.AWAIT_LOGIN) {

				// create loginmessage
				playerName = "Room237_" + randSeed.nextInt(100000);
				MazeCom loginmsg = mcFactory.createLoginMessage(playerName);

				// wait for Listener to receive answer
				/*
				 * 240 * 500 = 120'000 = 2 min. Entspricht
				 * de.fhaachen.mazenet.config.LOGINTIMEOUT Normal- und
				 * Finalkonfiguration
				 */
				sendMessageAndWait(loginmsg, 240, 500);

			}

			ErrorType loginReply = serverListener.getAnswerToLoginMessage();

			if (loginReply == ErrorType.NOERROR) {

				loginFailed = false;
				System.out.println(
						"Successfully logged in as: " + playerName + " with ID " + serverListener.getPlayerID());
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return loginFailed;

	}

	/**
	 * Sends a move if hasMoveToSend flag is true, and game has not ended.
	 */
	public void playGame() {

		System.out.println("client: playGame: entered method.");

		// play game until win
		while (serverListener.isAlive() && serverListener.noWinnerYet()) {

			if (!hasMoveToSend) {

				// wait for actor to makeMove and setMoveToSend
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			} else {

				// send
				/*
				 * 40 * 500 = 20'000 = 20 sec. Entspricht
				 * de.fhaachen.mazenet.config.SENDTIMEOUT Finalkonfiguration
				 */
				sendMessageAndWait(moveMsg, 40, 500);

				// find out if move accepted, else an unknown problem occurred:
				// exit
				ErrorType answerToMoveMessage = serverListener.getAnswerToMoveMessage();
				System.out.println("client: playGame: answerToMoveMessage: " + answerToMoveMessage.toString());
				switch (answerToMoveMessage) {

				case NOERROR:
					hasMoveToSend = false;
					break;

				case ILLEGAL_MOVE:
					hasMoveToSend = false;
					actor.requestMoveAgain(); // resets flag after processing
					break;

				case TOO_MANY_TRIES:
					System.out.println("Error: Too many move tries. Server cut cunnection. Exiting.");
					System.exit(1);

				default:
					System.out.println("Error: Could not determine move reply. Exiting.");
					System.exit(1);
				}

				/*
				 * only if move was successful: set hasMoveToSend flag back to
				 * false
				 */

			} // end if-else hasMoveToSend

		} // end while
	}

	/**
	 * Waits for serverListener to receive answer.
	 * 
	 * @param msg
	 *            Message to send to server
	 * @param intervals
	 *            number of intervals to wait for answer
	 * @param milliseconds
	 *            length of each interval
	 */
	private void sendMessageAndWait(MazeCom msg, int intervals, int milliseconds) {
		serverListener.setFlag_waitingForAnswer();
		System.out.println("Client: Sending message to server of type " + msg.getMcType().toString());
		outToServer.write(msg);
		int interval = 0;
		while (serverListener.getFlag_waitingForAnswer()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Error: Interrupted while sleeping. Exit.");
				System.exit(1);
			}
			interval++;
			if (interval == intervals) {
				System.out.println("Error: Waiting for server answer timed out. Exiting.");
				System.exit(1);
			}
			if (!serverListener.isAlive()) {
				System.out.println("Error: ServerListener closed unexpectedly. Exiting.");
				System.exit(1);
			}
		} // end while
	}

	private OutputStream getOutputStream() throws IOException {
		return socketToServer.getOutputStream();
	}

	public void initActor(int playerID) {

		switch (actorChoice) {
		case ACTORCHOICE_HUMAN:
			actor = new Human(this, playerID);
			break;
		case ACTORCHOICE_KI:
			actor = new Robot(this, playerID);
		}
		moveMsg = oFactory.createMazeCom();
		hasMoveToSend = false;

		actor.start();

	}

	public void makeMove(AwaitMoveMessageType awm) {

		actor.requestMove(awm);

	}

	/**
	 * Used by actor to alert client to send the move by activating
	 * hasMoveToSend flag.
	 * 
	 * @param move
	 */
	public void setMoveToSend(MoveMessageType move) {

		// package move into message
		moveMsg = oFactory.createMazeCom();
		moveMsg.setMcType(MazeComType.MOVE);
		moveMsg.setId(serverListener.getPlayerID());
		moveMsg.setMoveMessage(move);

		// alert client to send move
		this.actor.resetFlag_MoveRequest();
		this.hasMoveToSend = true;
	}

	public void resetFlag_hasMoveToSend() {
		hasMoveToSend = false;
	}

	public int getPlayerID() {
		return serverListener.getPlayerID();
	}

	public boolean noWinnerYet() {
		return serverListener.noWinnerYet();
	}

}
