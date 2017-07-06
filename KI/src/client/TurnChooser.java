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
		PositionType playerPosition = board.findPlayer(this.playerId);
		PositionType treasurePosition = board.findTreasure(turn.getTreasure());
		List<Position> possibleMoves = board.getAllReachablePositions(playerPosition);
		
		double distance = -1.0;
//		int moveIndex = -1;
		for(int i = 0; i < possibleMoves.size(); i++){
			int treasureX = treasurePosition.getRow();
			int treasureY = treasurePosition.getCol();
			int playerX = possibleMoves.get(i).getRow();
			int playerY = possibleMoves.get(i).getCol();
			
			double tmpDistance = Math.sqrt((double)(treasureX*playerX + treasureY*playerY));
			if(distance == -1.0 || distance > tmpDistance){
//				moveIndex = i;
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
		
		// returning the highest key (the best turn)
		return turns.get(keys.get(keys.size()));
	}
}
