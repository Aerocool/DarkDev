package client;


import java.io.IOException;
import java.net.Socket;

import javax.xml.bind.JAXBException;

import de.fhac.mazenet.server.generated.AcceptMessageType;
import de.fhac.mazenet.server.generated.ErrorType;
import de.fhac.mazenet.server.generated.LoginReplyMessageType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;
import de.fhac.mazenet.server.networking.XmlInStream;

/**
 * Diese Klasse horcht am Socket auf Nachrichten vom Server und leitet diese an
 * den TCPClient / (an die KI-Klasse) weiter.
 * 
 * TODO: Login-Prozess genauso wie auf ServerSeite mit eigenem Login-Thread
 * machen.
 * 
 * @author johannesw
 *
 */
public class ServerListener_JW extends Thread {

	/*
	 * Client- AND Server-related stuff
	 */
	boolean hasNewMoveRequest;

	/*
	 * Client-related stuff
	 */
	// this listener's client
	private TCPClient_JW client;
	// socket supplied by client
	private Socket socketClientSide;
	// used by client to init it's actor
	private int playerID;
	// used by client for login routine
	private ErrorType loginReply;
	// used by client for move routine
	private ErrorType moveReply;

	/*
	 * Internal stuff
	 */
	// internal validator
	private boolean noWinnerYet;

	/*
	 * Server- /(Incomning messages)- related stuff
	 */
	// message reader
	private XmlInStream inFromServer;
	// flag used by Client to check if new message has arrived
	private boolean isWaitingForAnswer;
	// for processing received server messages
	private MazeCom msg;
	private MazeComType msgType;

	public ServerListener_JW(TCPClient_JW client, Socket socketClientSide) throws IOException {

		this.client = client;
		this.socketClientSide = socketClientSide;
		init();

	}

	private void init() throws IOException {

		this.noWinnerYet = true;
		this.loginReply = ErrorType.AWAIT_LOGIN;

		this.inFromServer = new XmlInStream(socketClientSide.getInputStream());
		this.isWaitingForAnswer = false;

		this.msg = null;
		this.msgType = null;

		this.hasNewMoveRequest = false;

	}

	/**
	 * Handles all types of server responses and alerts {@link TCPClient_JW} to
	 * their arriving.
	 */
	public void run() {

		// Message Preconfiguration

		while (noWinnerYet) {

			try {
				msg = inFromServer.readMazeCom();
				// reset flag to inform client message has arrived
				isWaitingForAnswer = false;

				// determine message type
				msgType = msg.getMcType();
				System.out.println("ServerListener run: received msg of type "+msgType);

				// act according to messageType
				switch (msgType) {

				case ACCEPT:

					processAcceptMessage();
					break;

				case AWAITMOVE:

					processMoveRequest();
					break;

				case LOGINREPLY:

					processLoginReplyMessage();
					break;

				case DISCONNECT:
					processDisconnectMessage();
					break;

				case WIN:

					noWinnerYet = false;

					break;

				}

			} catch (IOException e) {
				System.out.println("ServerListener I/O problem, closed.");
				// noWinnerYet = false;
				// e.printStackTrace();
				break;
			} catch (JAXBException e1) {
				System.out.println("ServerListener JAXB problem, closed");
				// e1.printStackTrace();
				break;
			}
		} // end while
		System.out.println("ServerListener is offline");

		System.out.println("ServerListener stopping run");
	}// end run

	private void processLoginReplyMessage() {

		LoginReplyMessageType lrmsg = msg.getLoginReplyMessage();
		loginReply = ErrorType.NOERROR;
		playerID = lrmsg.getNewID();
		// setup actor for client to play game
		client.initActor(playerID);

	}

	/**
	 * Cases handled here:
	 * <p>
	 * <ul>
	 * <li>LoginMessage sent:
	 * <ul>
	 * <li>Login unsuccessful (isAccept FALSE, ErrorType AWAIT_LOGIN)</li>
	 * </ul>
	 * </li>
	 * <li>MoveMessage sent:
	 * <ul>
	 * <li>Move accepted (isAccept TRUE, ErrorType NOERROR)</li>
	 * <li>Move not accepted, falsche Nachricht (isAccept FALSE, ErrorType
	 * AWAIT_MOVE)</li>
	 * <li>Move not accepted, illegitimer Zug (isAccept FALSE, ErrorType
	 * ILLEGAL_MOVE)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 */
	private void processAcceptMessage() {

		AcceptMessageType acceptmsg = msg.getAcceptMessage();

		boolean isAccept = acceptmsg.isAccept();
		ErrorType errorType = acceptmsg.getErrorCode();

		if (isAccept) { // accepted MoveMessage

			switch (errorType) {
			
			case NOERROR:
				moveReply = errorType;
				break;

			default:
				System.out.println("Error: Serverlistener: processAcceptMessage: isAccept true: but unhandled ErrorType.");
				break;
			}

		} else {

			switch (errorType) {

			case AWAIT_LOGIN:
				loginReply = errorType;
				break;

			case AWAIT_MOVE:
				moveReply = errorType;
				break;

			case ILLEGAL_MOVE:
				moveReply = errorType;
				break;

			default:
				System.out.println("Error: Serverlistener: processAcceptMessage: isAccept false:  unhandled ErrorType.");
				break;
			}

		}

	}

	private void processMoveRequest() {

		hasNewMoveRequest = true;
		client.makeMove(msg.getAwaitMoveMessage());

	}

	private void processDisconnectMessage() {

		loginReply = msg.getDisconnectMessage().getErrorCode();
		moveReply = msg.getDisconnectMessage().getErrorCode();

	}

	/**
	 * Used by client to check if listener has received new message.
	 * 
	 * @return
	 */
	public boolean getFlag_waitingForAnswer() {
		return isWaitingForAnswer;
	}

	public void setFlag_waitingForAnswer() {
		isWaitingForAnswer = true;
	}

	/**
	 * Used by client to check if login was successful
	 * 
	 * @return
	 */
	public ErrorType getAnswerToLoginMessage() {
		return loginReply;
	}

	public ErrorType getAnswerToMoveMessage() {
		return moveReply;
	}

	public int getPlayerID() {
		return playerID;
	}

	public boolean noWinnerYet() {
		return noWinnerYet;
	}

}
