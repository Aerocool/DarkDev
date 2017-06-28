package client;


import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.ObjectFactory;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.networking.MazeComMessageFactory;

public class MazeComMessageFactoryExtended extends MazeComMessageFactory {
	
	static private ObjectFactory of = new ObjectFactory();

	public MazeComMessageFactoryExtended() {
		super();
	}
	
	public MoveMessageType createMoveMessage(CardType shiftCard, int shiftRow, int shiftCol, int pinRow, int pinCol){
		
		MoveMessageType mv = of.createMoveMessageType();
		mv.setShiftCard(shiftCard);
		PositionType p = of.createPositionType();
		p.setRow(shiftRow);
		p.setCol(shiftCol);
		mv.setShiftPosition(p);
		p = of.createPositionType();
		p.setRow(pinRow);
		p.setCol(pinCol);
		mv.setNewPinPos(p);
		
		return mv;
	}

}
