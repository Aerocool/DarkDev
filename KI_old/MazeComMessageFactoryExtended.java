package client;

import de.fhaachen.mazenet.generated.CardType;
import de.fhaachen.mazenet.generated.MazeCom;
import de.fhaachen.mazenet.generated.MazeComType;
import de.fhaachen.mazenet.generated.MoveMessageType;
import de.fhaachen.mazenet.generated.ObjectFactory;
import de.fhaachen.mazenet.generated.PositionType;
import de.fhaachen.mazenet.networking.MazeComMessageFactory;

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
