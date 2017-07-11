package KI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class TurnChooser {
	private final int playerId;
	private boolean simulateFurtherTurns = true;
	
	// Speichert alle legalen Züge
	private ArrayList<Turn> turns;
	
	public TurnChooser(int id){
		playerId = id;
		turns = new ArrayList<>();
	}
	
	// Fügt einen Zug dessen in die Liste der legalen Züge ein und bewertet diese
	public void addTurn(PositionType newCardPosition, TreasureType treasure,
			CardType card, BoardType board){
		if(newCardPosition == null || treasure == null || card == null || board == null)
			throw new NullPointerException("Turn konnte nicht initialisiert werden");
		Turn turn = new Turn(newCardPosition, treasure, card, board);
		turn.setRating(validateTurn(turn));
		turns.add(turn);
	}
	
	// Funktion um einen Zug zu bewerten
	public double validateTurn(Turn turn){
		// TODO
		Board board = new Board(turn.getBoard());
		if(board.getForbidden() != null)
			if((turn.getNewCardPosition().getCol() == board.getForbidden().getCol()) && (turn.getNewCardPosition().getRow() == board.getForbidden().getRow())) {
				return Double.MAX_VALUE;
			}
		MoveMessageType simulatedMove = new MoveMessageType();
		simulatedMove.setShiftPosition(turn.getNewCardPosition());
		simulatedMove.setNewPinPos(board.findPlayer(this.playerId));
		simulatedMove.setShiftCard(turn.getCard());
		board = board.fakeShift(simulatedMove);
		
		PositionType playerPosition = board.findPlayer(this.playerId);
		List<Position> possibleMoves = board.getAllReachablePositions(playerPosition);
		PositionType treasurePosition = board.findTreasure(turn.getTreasure());
		// falls durch das Verschieben die Schatzkarte zur Schiebekarte wird
		if(treasurePosition == null) {
			return Double.MAX_VALUE;
		}
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
//		if(distance != 0) {
//			if(playerPosition.getCol() == 0 || playerPosition.getCol() == 6 || playerPosition.getRow() == 0 || playerPosition.getRow() == 6)
//				distance += Double.MIN_VALUE;
//		}
		return distance;
	}
	
	// Ermittelt den Bestmöglichen Zug (Rekursionstiefe im Standardfall 2)
	public Turn getBestTurn() {
		ArrayList<Turn> bestTurns = getBestTurns();
		if (bestTurns.size() > 0) {
			if (this.simulateFurtherTurns == true) {
				bestTurns = simulateFurtherTurns(bestTurns);
				bestTurns = eliminateBadShiftCards(bestTurns);
				bestTurns = improveReachablePositions(bestTurns);
				bestTurns = reduceReachablePositionsForEnemies(bestTurns);
				bestTurns = getTurnsToDisturbEnemy(bestTurns);
			}
		}
		return bestTurns.get(0);
	}

	private ArrayList<Turn> improveReachablePositions(ArrayList<Turn> bestTurns) {
		int reachablePositions = -1;
		ArrayList<Turn> moreReachablePositions = new ArrayList<Turn>();
		for(Turn turn : bestTurns) {
			Board board = new Board(turn.getBoard());
			PositionType positionsPlayer = board.findPlayer(this.playerId);
			MoveMessageType simulatedMove = new MoveMessageType();
			simulatedMove.setShiftPosition(turn.getNewCardPosition());
			simulatedMove.setNewPinPos(positionsPlayer);
			simulatedMove.setShiftCard(turn.getCard());
			board = board.fakeShift(simulatedMove);

			if(reachablePositions == board.getAllReachablePositions(positionsPlayer).size()) {
				moreReachablePositions.add(turn);
			}
			if(board.getAllReachablePositions(positionsPlayer).size() > reachablePositions) {
				reachablePositions = board.getAllReachablePositions(positionsPlayer).size();
				moreReachablePositions.clear();
				moreReachablePositions.add(turn);
			}
		}
		return moreReachablePositions;
	}

	private ArrayList<Turn> reduceReachablePositionsForEnemies(ArrayList<Turn> bestTurns) {
		int openingsSum = 0;
		int openings = Integer.MAX_VALUE;
		ArrayList<Turn> enemyDisturb = new ArrayList<Turn>();
		for(Turn turn : bestTurns) {
			Board board = new Board(turn.getBoard());
			
			for(int i = 1; i < 5; i++) {
				if(this.playerId == i)
					continue;
				PositionType positionEnemy = board.findPlayer(i);
				MoveMessageType simulatedMove = new MoveMessageType();
				simulatedMove.setShiftPosition(turn.getNewCardPosition());
				simulatedMove.setNewPinPos(positionEnemy);
				simulatedMove.setShiftCard(turn.getCard());
				board = board.fakeShift(simulatedMove);
				openingsSum += board.getAllReachablePositions(positionEnemy).size();
			}
			
			if(openingsSum == openings) {
				enemyDisturb.add(turn);
			}
			if(openingsSum < openings) {
				enemyDisturb.clear();
				enemyDisturb.add(turn);
				openings = openingsSum;
			}
			openingsSum = 0;
		}
		return enemyDisturb;
	}

	// Gibt alle Züge zurück, deren Shiftkarte eine "T-Karte" ist
	private ArrayList<Turn> eliminateBadShiftCards(ArrayList<Turn> bestTurns) {
		ArrayList<Turn> filteredTurns = new ArrayList<Turn>();
		for(Turn turn : bestTurns) {
			if(turn.getCardOpenings() == 3) {
				filteredTurns.add(turn);
			}
		}
		if(filteredTurns.size() == 0) {
			return bestTurns;
		} else {
			return filteredTurns;
		}
	}

	// Gibt alle Bestmöglichen Züge aus der Liste der legalen Züge zurück, die gleichwertig sind
	private ArrayList<Turn> getBestTurns(){
		ArrayList<Turn> bestTurns = new ArrayList<>();
		double lowestDistance = getLowestDistance(turns);
		for(Turn turn : turns) 
			if(turn.getRating() == lowestDistance)
				bestTurns.add(turn);
		return bestTurns;
	}
	
	
	// Ermittelt die kürzeste Distanz zum Schatz, die nach Ausführung eines Zuges möglich ist
	private double getLowestDistance(ArrayList<Turn> turns) {
		double lowestDistance = Double.MAX_VALUE;
		for(Turn turn : turns) {
			if(turn.getRating() < lowestDistance) {
				lowestDistance = turn.getRating();
			}
		}
		return lowestDistance;
	}
	
	public void deleteAllTurns() {
		turns.clear();
	}
	
	// Gibt alle Züge zurück, die den Gegner behindern
	private ArrayList<Turn> getTurnsToDisturbEnemy(ArrayList<Turn> equalDistanceTurns) {
		// scanning if there are enemies at the edge
		ArrayList<Turn> turns = new ArrayList<Turn>();
		Board board = new Board(equalDistanceTurns.get(0).getBoard());
		
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
						turns.add(newTurn);
					}
				}
			}
		}
		if(turns.size() == 0)
			return equalDistanceTurns;
		return turns;
	}
	
	// Gibt den Zug zurück, der auf gegebene Position gesetzt wird (dessen Schiebekarte auf die Position gesetzt werden soll)
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
	
	// Simuliert welcher Zug geeigneter ist in Anbetracht des weiteren Spielverlaufs (Störungen durch andere Spieler werden nicht berücksichtigt)
	private ArrayList<Turn> simulateFurtherTurns(ArrayList<Turn> turns){
		if(simulateFurtherTurns == false)
			return turns;
		
		// Wenn schon ein Weg zum Ziel gefunden worden ist, muss dieser nicht mehr gesucht werden
		if(getLowestDistance(turns) == 0.0)
			return turns;
		
		return iterateOverTurnsForSimulation(turns, 3);
	}
	
	private ArrayList<Turn> iterateOverTurnsForSimulation(ArrayList<Turn> turns, int recursionDepth){
		if(recursionDepth == 0)
			return turns;
		
		HashMap<Turn, Turn> simulatedTurns = new HashMap<>();
		ArrayList<Turn> bestTurns = new ArrayList<Turn>();
		AwaitMoveMessageType awaitMoveMessage = new AwaitMoveMessageType();
		double distanceSecond = Double.MAX_VALUE;
		
		for(int i = 0; i < turns.size(); i++) {
			Turn turn = turns.get(i);
			awaitMoveMessage.setTreasure(turn.getTreasure());
			
			Board board = new Board(turn.getBoard());
			MoveMessageType simulatedMove = new MoveMessageType();
			simulatedMove.setShiftPosition(turn.getNewCardPosition());
			simulatedMove.setNewPinPos(board.findPlayer(this.playerId));
			simulatedMove.setShiftCard(turn.getCard());
			board.setTreasure(turn.getTreasure());
			board = board.fakeShift(simulatedMove);
			
			KIThilo KI = new KIThilo(this.playerId);
			KI.setSimulateFurtherTurns(false);
			Turn secondTurn = KI.getNextMove(board);
			secondTurn.setRating(validateTurn(secondTurn));
			
			if(secondTurn.getRating() == distanceSecond) {
				simulatedTurns.put(secondTurn, turn);
			}
			if(turn.getRating() >= secondTurn.getRating() && secondTurn.getRating() <= distanceSecond) {
				bestTurns.clear();
				distanceSecond = secondTurn.getRating();
				simulatedTurns.clear();
				simulatedTurns.put(secondTurn, turn);
			}
			
		}
		if(simulatedTurns.size() > 0) {
			Set<Turn> bestSecondTurns = simulatedTurns.keySet(); 
			
			ArrayList<Turn> filteredSecondTurns = new ArrayList<Turn>();
			filteredSecondTurns.addAll(bestSecondTurns);
			filteredSecondTurns = iterateOverTurnsForSimulation(filteredSecondTurns, recursionDepth-1);
			Iterator<Turn> it  = filteredSecondTurns.iterator();
			
			while(it.hasNext()) {
				bestTurns.add(simulatedTurns.get(it.next()));
			}
			return bestTurns;
		}
		return turns;
	}
	
	// Legt fest, ob weitere mögliche Züge bei der Auwahl des besten Zuges simuliert werden sollen
	public void setSimulateFurtherTurns(boolean simulation) {
		this.simulateFurtherTurns = simulation;
	}
	
	public boolean getSimulateFurtherTurns() {
		return this.simulateFurtherTurns;
	}
	
	// Gibt zurück, over der üvergebene Zug verboten ist
	private boolean isForbidden(Turn turn) {
		PositionType position = turn.getBoard().getForbidden();
		PositionType turnPosition = turn.getNewCardPosition();
		if(position.getCol() == turnPosition.getCol() && position.getRow() == turnPosition.getRow())
			return true;
		return false;
	}
	
	private ArrayList<Turn> listToArrayList(List<Turn> list){
		ArrayList<Turn> arrayList = new ArrayList<Turn>();
		arrayList.addAll(list);
		return arrayList;
	}
}
