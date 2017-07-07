package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Position;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class TurnChooser {
	private final int playerId;
	
	// Double = value of the turn (the higher the better)
	private HashMap<Double, Turn> turns;
	
	public TurnChooser(int id){
		playerId = id;
		turns = new HashMap<>();
	}
	
	public void addTurn(PositionType newCardPosition, TreasureType treasure,
			CardType card, BoardType board){
		Turn turn = new Turn(newCardPosition, treasure, card, board);
		turns.put(validateTurn(turn), turn);
	}
	
	private double validateTurn(Turn turn){
		// TODO
		Board board = new Board(turn.getBoard());
		if(board.getForbidden() != null)
			if((turn.getNewCardPosition().getCol() == board.getForbidden().getCol()) && (turn.getNewCardPosition().getRow() == board.getForbidden().getRow()))
				return Double.MAX_VALUE;
		MoveMessageType simulatedMove = new MoveMessageType();
		simulatedMove.setShiftPosition(turn.getNewCardPosition());
		simulatedMove.setNewPinPos(board.findPlayer(this.playerId));
		simulatedMove.setShiftCard(turn.getCard());
		board = board.fakeShift(simulatedMove);
		
		PositionType playerPosition = board.findPlayer(this.playerId);
		List<Position> possibleMoves = board.getAllReachablePositions(playerPosition);
		PositionType treasurePosition = board.findTreasure(turn.getTreasure());
		
		double distance = -1.0;
		
		for(int i = 0; i < possibleMoves.size(); i++){
			int treasureX = treasurePosition.getRow();
			int treasureY = treasurePosition.getCol();
			int playerX = possibleMoves.get(i).getRow();
			int playerY = possibleMoves.get(i).getCol();
			
			int distanceVectorX = treasureX - playerX;
			int distanceVectorY = treasureY - playerY;
			double tmpDistance = Math.sqrt((double)(distanceVectorX*distanceVectorX+ distanceVectorY*distanceVectorY));
			if(distance == -1.0 || distance > tmpDistance){
				distance = tmpDistance;
			}
		}
		return distance;
	}
	
	public Turn getBestTurn(){
		// Converting Keys to a List and sort them
		List<Double> keys = new ArrayList<Double>();
		keys.addAll(turns.keySet());
		
		Collections.sort(keys);
		if(keys.get(0) != null && keys.get(1) != null) {
			if(keys.get(0) == keys.get(1)) { // falls es mehrere gleichwertige Züge gibt, hinsichtlich der geminderten Entfernung zum Schatz
				System.out.println("Mehrere Gleichwertige");
				ArrayList<Turn> equalDistanceTurns = new ArrayList<>();
				for(int i = 0; i < keys.size(); i++) {
					if(keys.get(0) == keys.get(i)) {
						equalDistanceTurns.add(turns.get(keys.get(i)));
					} else {
						break;
					}
				}
				return getTurnToDisturbEnemy(equalDistanceTurns);
			}
		}
		return turns.get(keys.get(0));
	}
	
	public void deleteAllTurns() {
		turns.clear();
	}
	
	private Turn getTurnToDisturbEnemy(ArrayList<Turn> equalDistanceTurns) {
		// scanning if there are enemies at the edge
		Board board = new Board(equalDistanceTurns.get(0).getBoard());
		Turn turn = equalDistanceTurns.get(0);
		for(int i = 1; i < 5; i++) {
			if(i == playerId)
				continue;
			PositionType enemyPosition = board.findPlayer(i);
			if(enemyPosition.getCol() == 0 || enemyPosition.getCol() == 6
					|| enemyPosition.getRow() == 0 || enemyPosition.getRow() == 6) {
				// Falls sich der Gegegner auf einen der Ecken befindet
				if((enemyPosition.getCol() == 0 && enemyPosition.getRow() == 0) || (enemyPosition.getCol() == 6 && enemyPosition.getRow() == 0 )
						|| (enemyPosition.getCol() == 0 && enemyPosition.getRow() == 6) || (enemyPosition.getCol() == 6 && enemyPosition.getRow() == 6)) {
					continue;
				} else
				{
					// Überprüfen ob einer der Züge den Gegner aus dem Spielfeld schieben kann
					Turn newTurn = null;
					
					if(enemyPosition.getCol() == 0) {
						newTurn = checkIfTurnsContainPosition(KIThilo.createPositionType(6, enemyPosition.getRow()), equalDistanceTurns);
					} else if(enemyPosition.getCol() == 6) {
						newTurn = checkIfTurnsContainPosition(KIThilo.createPositionType(0, enemyPosition.getRow()), equalDistanceTurns);
					} else if(enemyPosition.getRow() == 0) {
						newTurn = checkIfTurnsContainPosition(KIThilo.createPositionType(enemyPosition.getCol(), 6), equalDistanceTurns);
					} else if(enemyPosition.getRow() == 6) {
						newTurn = checkIfTurnsContainPosition(KIThilo.createPositionType(enemyPosition.getCol(), 0), equalDistanceTurns);
					}
					
					if(newTurn != null) {
						System.out.println("Neuer Zug gesetzt!");
						turn = newTurn;
					}
				}
			}
		}
		return turn;
	}
	
	private Turn checkIfTurnsContainPosition(PositionType shiftPosition, ArrayList<Turn> turns) {
		PositionType position;
		for(int i = 0; i < turns.size(); i++) {
			position = KIThilo.createPositionType(turns.get(i).getNewCardPosition().getCol(), turns.get(i).getNewCardPosition().getRow());
			if(position.getCol() == shiftPosition.getCol() && position.getRow() == shiftPosition.getRow()) {
				return turns.get(i);
			}
		}
		return null;
	}
}
