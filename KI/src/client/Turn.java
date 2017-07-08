package client;

import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class Turn {
	private PositionType newCardPosition;
	private TreasureType treasure;
	private CardType card;
	private BoardType board;
	private double rating = -1.;
	
	public Turn(PositionType newCardPosition, TreasureType treasure, CardType card, BoardType board, double rating){
		this.newCardPosition = newCardPosition;
		this.treasure = treasure;
		this.card = card;
		this.board = board;
		this.rating = rating;
	}
	
	public Turn(PositionType newCardPosition, TreasureType treasure, CardType card, BoardType board){
		this.newCardPosition = newCardPosition;
		this.treasure = treasure;
		this.card = card;
		this.board = board;
	}
	
	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
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
		return "(" + newCardPosition.getCol() + ";" + newCardPosition.getRow() + "), distance:" + this.getRating();
	}
}
