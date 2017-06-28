package client;



import java.util.List;
import java.util.Random;

import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.CardType.Openings;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Position;



public class Robot extends Actor  {
	private PositionType ownPosition;
	private PositionType treasurePosition;
	private MoveMessageType move;
	private List<Position> possibleMoves;
	private Board extBoard;
	

	public Robot(TCPClient_JW client, int id) {
		super(client, id);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		while (client.noWinnerYet()) {
			if (newMoveRequested) {
				// determine next KI Move

				// extract awm
				board = current_awm.getBoard();
				treasuresToGo = current_awm.getTreasuresToGo();
				foundTreasures = current_awm.getFoundTreasures();
				treasure = current_awm.getTreasure();
				
				//init Board-Type extBoard
				extBoard = new Board(board);
				
				//init move
				move = of.createMoveMessageType();


				ownPosition = extBoard.findPlayer( this.id );
				treasurePosition = extBoard.findTreasure( this.treasure );

				try {
					makeFakeMove();
					client.setMoveToSend( move );
				}
				catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}  else {
				// wait for client to requestMove
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public void makeFakeMove() throws InterruptedException{

		move.setShiftCard(rotateShiftCard( randInt(0,4)));    // hier noch ein random int 0 bis 4 spendieren
		PositionType randommove = of.createPositionType();

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
			//try {
				extBoard = extBoard.fakeShift(move);
			/*}
			catch ( CloneNotSupportedException e ) {
				System.out.println("Invalid Shift");// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			ownPosition = extBoard.findPlayer(this.id);
			possibleMoves =  extBoard.getAllReachablePositions(ownPosition);	
			int iBestMove = bestenZugBerechnen( treasurePosition, possibleMoves); 
			if(extBoard.pathPossible(ownPosition, possibleMoves.get(iBestMove))){
				move.setNewPinPos(possibleMoves.get(iBestMove));
				break;
			}
		}
	}
	
	public int bestenZugBerechnen(PositionType treasurePosition, List<Position> allemoeglichen ){
		 
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

	public CardType rotateShiftCard( int times ){

		CardType shiftCard = board.getShiftCard();
		Openings cardOpenings = of.createCardTypeOpenings();  	
		// Drehung der Shiftkarte
		for( int i = 0; i < times; ++i ){
			cardOpenings = of.createCardTypeOpenings();			
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