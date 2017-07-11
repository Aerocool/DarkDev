package KI;

import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.CardType.Openings;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class Turn {
	private PositionType newCardPosition;
	private TreasureType treasure;
	private CardType card;
	private BoardType board;
	private double rating;
	private int cardOpenings;
	
	public Turn(PositionType newCardPosition, TreasureType treasure, CardType card, BoardType board, double distance){
		this.newCardPosition = newCardPosition;
		this.treasure = treasure;
		this.card = card;
		this.board = board;
		this.rating = distance;
		this.cardOpenings = determineCardOpenings();
	}
	
	// Emittelt wieviele Öffnungen die Karte hat
	private int determineCardOpenings() {
		Openings open = card.getOpenings();
		if((open.isTop() == true && open.isBottom() == true && open.isLeft() == false && open.isRight() == false)
				|| (open.isTop() == true && open.isBottom() == true && open.isLeft() == false && open.isRight() == false))
			return 2;
		else
			return 3;
	}
	
	public Turn(PositionType newCardPosition, TreasureType treasure, CardType card, BoardType board){
		this.newCardPosition = newCardPosition;
		this.treasure = treasure;
		this.card = card;
		this.board = board;
		this.cardOpenings = determineCardOpenings();
	}
	
	public double getRating() {
		return rating;
	}
	
	public void setRating(double rating) {
		this.rating = rating;
	}
	
	public int getCardOpenings() {
		return this.cardOpenings;
	}
	
	public PositionType getNewCardPosition() {
		return newCardPosition;
	}
	public void setNewCardPosition(PositionType newCardPosition) {
		this.newCardPosition = newCardPosition;
	}
	public TreasureType getTreasure() {
		return treasure;
	}
	public void setTreasure(TreasureType treasure) {
		this.treasure = treasure;
	}
	public CardType getCard() {
		return card;
	}
	public void setCard(CardType card) {
		this.card = card;
	}
	public BoardType getBoard() {
		return board;
	}
	public void setBoard(BoardType board) {
		this.board = board;
	}
	public String toString() {
		return "(" + newCardPosition.getCol() + ";" + newCardPosition.getRow() + "), distance:" + this.rating;
	}
}
