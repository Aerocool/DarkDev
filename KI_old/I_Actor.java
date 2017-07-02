package client;

import de.fhaachen.mazenet.generated.AwaitMoveMessageType;
import de.fhaachen.mazenet.generated.MoveMessageType;

/**
 * Interface for an actor. Can be a KI, or a human.
 * @author johannesw
 *
 */
public interface I_Actor {
	
	public void makeMove(AwaitMoveMessageType awm);
	
	public void remakeMove();
	
	//used by Client to tell if Actor has made a new move
	public int getMadeMovesCount();

}
