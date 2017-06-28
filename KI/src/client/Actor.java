package client;


import java.util.List;

import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.ObjectFactory;
import de.fhac.mazenet.server.generated.TreasureType;
import de.fhac.mazenet.server.generated.TreasuresToGoType;

public abstract class Actor extends Thread {

	protected int id;
	protected TCPClient_JW client;

	// stuff from the AwaitMoveMessage
	protected AwaitMoveMessageType current_awm; // important for method
												// remakeMove()
	protected BoardType board;
	protected List<TreasuresToGoType> treasuresToGo;
	protected List<TreasureType> foundTreasures;
	protected TreasureType treasure;
	//
	protected MazeComMessageFactoryExtended mcmfe;
	protected ObjectFactory of;
	protected MoveMessageType mv;
	protected boolean newMoveRequested;

	public Actor(TCPClient_JW client, int id) {

		this.client = client;
		this.id = id;
		mcmfe = new MazeComMessageFactoryExtended();
		of = new ObjectFactory();
		mv = new MoveMessageType();
		current_awm = of.createAwaitMoveMessageType();

		newMoveRequested = false;

	}

	/**
	 * Buffer awaitmovemessage internally and set flag for move requested.
	 * Implemented actor then will process request in it's thread run method.
	 * 
	 * @param awm
	 */
	public void requestMove(AwaitMoveMessageType awm) {
		current_awm = awm;
		newMoveRequested = true;
	}
	
	/**
	 * Request another move for the same AwaitMoveMessage.
	 * Last move was not accepted.
	 * Ideally, this would guarantee to make a different move (e.g. randomized).
	 * 
	 */
	public void requestMoveAgain() {
		requestMove(current_awm);
	}

	
	public void resetFlag_MoveRequest() {
		newMoveRequested = false;
	}
	


}
