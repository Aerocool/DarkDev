package client;

import java.util.List;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;
import de.fhac.mazenet.server.generated.TreasuresToGoType;
import de.fhac.mazenet.server.generated.CardType.Openings;

public class KIThilo
implements ArtificialIntelligence {
	private final int id;
	
	private PositionType ownPosition;
	private PositionType treasurePosition;
	private MoveMessageType move;
	private Board extBoard;
	
	private BoardType board;
	private List<TreasuresToGoType> treasuresToGo;
	private List<TreasureType> foundTreasures;
	
	private TreasureType treasure;
	private TurnChooser turnChooser;
	
	public KIThilo(int id) {this.id = id;}
	
	private void initComponents(AwaitMoveMessageType awaitMoveMessage){
		board = awaitMoveMessage.getBoard();
		treasuresToGo = awaitMoveMessage.getTreasuresToGo();
		foundTreasures = awaitMoveMessage.getFoundTreasures();
		treasure = awaitMoveMessage.getTreasure();
		extBoard = new Board(board);
		ownPosition = extBoard.findPlayer( this.id );
		treasurePosition = extBoard.findTreasure( this.treasure );
		turnChooser = new TurnChooser(this.id);
	}

	@Override
	public MoveMessageType getNextMove(AwaitMoveMessageType awaitMoveMessage) {
		initComponents(awaitMoveMessage);
		
//		PositionType testPosition = new PositionType();
//		testPosition.setRow(0);
//		testPosition.setCol(1);
		
		MoveMessageType move = new MoveMessageType();
		PositionType position = new PositionType();
		CardType card = board.getShiftCard();
		Openings openings = new Openings();
		for(int i = 0; i < 4; i++){
			if(i == 0){
				openings.setBottom(true);
			} else if(i == 1){
				openings.setLeft(true);
			} else if(i == 2){
				openings.setRight(true);
			} else if(i == 3){
				openings.setTop(true);
			}
			card.setOpenings(openings);
			for(int col = 0; col < 7; col++){
				position.setCol(col);
				if(col == 0 || col == 6){
					for(int row = 1; row < 6; row += 2){
						position.setRow(row);
						turnChooser.addTurn(position, treasure, card, board);
					}
				} else if(col == 1 || col == 3 || col == 5){
					position.setRow(0);
					turnChooser.addTurn(position, treasure, card, board);
					position.setRow(6);
					turnChooser.addTurn(position, treasure, card, board);
				}
			}
		}
		Turn turn = turnChooser.getBestTurn();
		position = turn.getNewCardPosition();
		card = turn.getCard();
		
		extBoard = extBoard.fakeShift(move);
		ownPosition = extBoard.findPlayer(this.id);
		List<Position> possibleMoves =  extBoard.getAllReachablePositions(ownPosition);
		
		move.setShiftPosition(position);
		move.setShiftCard(card);
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
}
