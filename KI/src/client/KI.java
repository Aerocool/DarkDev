package client;

import java.util.List;
import java.util.Random;

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

public class KI {
	private AwaitMoveMessageType awaitMoveMessage;
	
	private PositionType ownPosition;
	private PositionType treasurePosition;
	private MoveMessageType move;
	private List<Position> possibleMoves;
	private Board extBoard;
	
	private BoardType board;
	private List<TreasuresToGoType> treasuresToGo;
	private List<TreasureType> foundTreasures;
	
	private int id;
	private TreasureType treasure;
	
	public KI(AwaitMoveMessageType amm, int id) {
		awaitMoveMessage = amm;
		this.id = id;
		
		board = awaitMoveMessage.getBoard();
		treasuresToGo = awaitMoveMessage.getTreasuresToGo();
		foundTreasures = awaitMoveMessage.getFoundTreasures();
		treasure = awaitMoveMessage.getTreasure();
		
		extBoard = new Board(board);
		
		move = new MoveMessageType();

		ownPosition = extBoard.findPlayer( this.id );
		treasurePosition = extBoard.findTreasure( this.treasure );
		
		makeFakeMove();
	}
	
	public MoveMessageType getMove() {
		return move;
	}
	
	private void makeFakeMove() {
		move.setShiftCard(rotateShiftCard( randInt(0,4)));    // hier noch einen random int von 0 bis 4 spendieren
		PositionType randommove = new PositionType();

		int auswahl = randInt(0, 2);
		while( true){
			if(auswahl == 1){	
				while( true ){
					int randomized = randInt(1, 6);
					if( randomized == 1 ||randomized ==  3 || randomized == 5 ){
						randommove.setCol(randomized);
						break;
					}
				}		
				while( true ){
					int randomized =  randInt(0, 7);
					if( randomized == 0 ||randomized ==  6 ){
						randommove.setRow(randomized);
						break;
					}
				}
			} else {
				while( true ){
					int randomized = randInt(1, 6);
					if( randomized == 1 ||randomized ==  3 || randomized == 5 ){
						randommove.setRow(randomized);
						break;
					}
				}

				while( true ){
					int randomized =  randInt(0, 7);
					if( randomized == 0 ||randomized ==  6 ){
						randommove.setCol(randomized);
						break;
					}
				}			
			}		
			move.setShiftPosition(randommove);	
			move.setNewPinPos(ownPosition);
			extBoard = extBoard.fakeShift(move);
			ownPosition = extBoard.findPlayer(this.id);
			possibleMoves =  extBoard.getAllReachablePositions(ownPosition);	
			int iBestMove = bestenZugBerechnen( treasurePosition, possibleMoves); 
			if(extBoard.pathPossible(ownPosition, possibleMoves.get(iBestMove))) {
				move.setNewPinPos(possibleMoves.get(iBestMove));
				break;
			}
		}
	}
	
	private int bestenZugBerechnen(PositionType treasurePosition, List<Position> allemoeglichen ) {
		 if( allemoeglichen.size() == 1 ){
			 return 0;
		 }	 
		 int xdiff = 0; 
		 int ydiff = 0;
		 double erg = 0.0;
		 int auswahl = -1;
		 
		 for( int i = 0; i < allemoeglichen.size(); ++i ){
			 if( treasurePosition != null) {
			 xdiff = allemoeglichen.get(i).getRow()-treasurePosition.getRow();
			 ydiff = allemoeglichen.get(i).getCol()-treasurePosition.getCol();
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
		 return auswahl;
	 }


	public static int randInt(int min, int max) {
		Random rand = new Random();
		int randomNum = rand.nextInt((max - min)) + min;
		return randomNum;	    
	}

	public CardType rotateShiftCard( int times ) {
		CardType shiftCard = board.getShiftCard();
		Openings cardOpenings = new Openings();  	
		// Drehung der Shiftkarte
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
}
