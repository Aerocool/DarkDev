package KI;

import de.fhac.mazenet.server.Position;

public class Enemy {
	private final int id;
	private int foundedTreasures;
	private Position currentPosition;
	
	public Enemy(int playerId) {
		this.id = playerId;
	}
	
	public int getFoundedTreasures() {
		return foundedTreasures;
	}

	public void setFoundedTreasures(int foundedTreasures) {
		this.foundedTreasures = foundedTreasures;
	}

	public Position getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(Position currentPosition) {
		this.currentPosition = currentPosition;
	}

	public int getId() {
		return id;
	}
}
