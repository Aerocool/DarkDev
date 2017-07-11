package KI;

import java.util.List;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.CardType.Openings;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class KIThilo
implements ArtificialIntelligence {
	private final int id;
	
	private PositionType ownPosition;
	private PositionType treasurePosition;
	private Board extBoard;
	
	private BoardType board;
	
	private TreasureType treasure;
	private TurnChooser turnChooser;
	
	public KIThilo(int id) {
		this.id = id;
		turnChooser = new TurnChooser(this.id);
	}
	
	private void initComponents(AwaitMoveMessageType awaitMoveMessage){
		board = awaitMoveMessage.getBoard();
		treasure = awaitMoveMessage.getTreasure();
		extBoard = new Board(board);
		
		ownPosition = extBoard.findPlayer(this.id);
		treasurePosition = extBoard.findTreasure(this.treasure);
//		updateEnemyInformation();
	}
	
	public Turn getNextMove(Board extBoard) {
		AwaitMoveMessageType awaitMoveMessage = new AwaitMoveMessageType();
		awaitMoveMessage.setBoard(extBoard);
		awaitMoveMessage.setTreasure(extBoard.getTreasure());
		MoveMessageType moveMessage = getNextMove(awaitMoveMessage);
		Turn turn = new Turn(moveMessage.getShiftPosition(), extBoard.getTreasure(), moveMessage.getShiftCard(), extBoard);
		return turn;
	}

	@Override
	public MoveMessageType getNextMove(AwaitMoveMessageType awaitMoveMessage) {
		initComponents(awaitMoveMessage);
		
		MoveMessageType move = new MoveMessageType();
		PositionType position = new PositionType();
		CardType card = board.getShiftCard();
		
		for(int i = 0; i < 4; i++){
			if(i == 0){
				card = rotateShiftCard(1, card);
			} else if(i == 1){
				card = rotateShiftCard(2, card);
			} else if(i == 2){
				card = rotateShiftCard(3, card);
			} else if(i == 3){
				card = rotateShiftCard(4, card);
			}
			for(int col = 0; col < 7; col++){
				position.setCol(col);
				if(col == 0 || col == 6){
					for(int row = 1; row < 6; row += 2){
						position.setRow(row);
						turnChooser.addTurn(copyPositionType(position), treasure, card, board);
					}
				} else if(col == 1 || col == 3 || col == 5){
					position.setRow(0);
					turnChooser.addTurn(copyPositionType(position), treasure, card, board);
					position.setRow(6);
					turnChooser.addTurn(copyPositionType(position), treasure, card, board);
				}
			}
		}
		Turn turn = turnChooser.getBestTurn();
		turnChooser.deleteAllTurns();
		position = turn.getNewCardPosition();
		card = turn.getCard();
		
		move.setShiftPosition(position);
		move.setShiftCard(card);
		
		extBoard = extBoard.fakeShift(move);
		ownPosition = extBoard.findPlayer(this.id);
		List<Position> possibleMoves = extBoard.getAllReachablePositions(ownPosition);
		
		move.setNewPinPos(getPlayerMovement(treasure, possibleMoves));
		return move;
	}
	
	// TODO
	private Position getPlayerMovement(TreasureType treasure, List<Position> possibleMoves){
		if( possibleMoves.size() == 1 ){
			 return possibleMoves.get(0);
		 }	 
		 int xdiff = 0; 
		 int ydiff = 0;
		 double erg = 0.0;
		 int auswahl = -1;
		 
		 for( int i = 0; i < possibleMoves.size(); ++i ){
			 treasurePosition = extBoard.findTreasure(treasure);
			 if( treasurePosition != null) {
			 xdiff = possibleMoves.get(i).getRow()-treasurePosition.getRow();
			 ydiff = possibleMoves.get(i).getCol()-treasurePosition.getCol();
			 double temp  = Math.sqrt( xdiff*xdiff+ydiff*ydiff);
			 if( i == 0 ){
				 erg = temp;
				 auswahl = 0;		
				 continue;
			 }		 
			 if( temp < erg ){
				 erg = temp;
				 auswahl = i;
			 }
			 } else {		 
				 auswahl = 0;
			 }
		 }
		 return possibleMoves.get(auswahl);
	}
	
	private static CardType rotateShiftCard(int times, CardType shiftCard) {
		Openings cardOpenings = new Openings();  	
		// Drehung der Karte
		for( int i = 0; i < times; ++i ){
			cardOpenings = new Openings();			
			if(shiftCard.getOpenings().isTop()){
				cardOpenings.setRight(true);
			} 
			if(shiftCard.getOpenings().isRight()){
				cardOpenings.setBottom(true);
			} 
			if(shiftCard.getOpenings().isBottom()){
				cardOpenings.setLeft(true);
			} 
			if(shiftCard.getOpenings().isLeft()){
				cardOpenings.setTop(true);
			} 
			shiftCard.setOpenings(cardOpenings);
		}	
		return shiftCard;
	}
	
	public static PositionType copyPositionType(PositionType position) {
		PositionType copy = new PositionType();
		copy.setCol(position.getCol());
		copy.setRow(position.getRow());
		return copy;
	}
	
	public static PositionType createPositionType(int col, int row) {
		PositionType position = new PositionType();
		position.setCol(col);
		position.setRow(row);
		return position;
	}
	
	public void setSimulateFurtherTurns(boolean simulation) {
		this.turnChooser.setSimulateFurtherTurns(simulation);
	}
	
	public boolean getSimulateFurtherTurns() {
		return this.turnChooser.getSimulateFurtherTurns();
	}
	
//	private void updateEnemyInformation() {
//		if(enemies.length == 0)
//			enemies = new Enemy[4];
//		for(int i = 1; i < 5; i++) {
//			if(i == id)
//				continue;
//			if(enemies[i] == null) {
//				enemies[i] = new Enemy(i);
//				enemies[i].setFoundedTreasures(0);
//			}
//			
//			// Position aktualisieren
//			Position position = extBoard.findPlayer(i);
//			enemies[i].setCurrentPosition(new Position(position));
//			
//			// rausfinden ob neue Schätze gefunden worden sind
//		}
//	}
}
