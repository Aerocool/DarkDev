package KI;

import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.MoveMessageType;

public interface ArtificialIntelligence {
	public MoveMessageType getNextMove(AwaitMoveMessageType awaitMoveMessage);
}
