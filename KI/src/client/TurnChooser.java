package client;

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
			throw new NullPointerException("Turn kann nicht initialisiert werden");
		Turn turn = new Turn(newCardPosition, treasure, card, board);
		turn.setRating(validateTurn(turn));
		turns.add(turn);
	}
	
	// Funktion um einen Zug zu bewerten
	public double validateTurn(Turn turn){
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
		return distance;
	}
	
	// Ermittelt den Bestmöglichen Zug (Rekursionstiefe im Standardfall 2)
	public Turn getBestTurn() {
		ArrayList<Turn> bestTurns = getBestTurns();

		if(this.simulateFurtherTurns == true) {
			if (bestTurns.size() > 1) {
//				bestTurns = getTurnsToDisturbEnemy(bestTurns);
				if(bestTurns.size() > 1 && this.simulateFurtherTurns == true) {
					for(int i = 0; i < 3; i++) {
						bestTurns = simulateFurtherTurns(bestTurns);
					}
				}
			}
		}
		return bestTurns.get(0);
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
						System.out.println("Neuer Zug gesetzt!");
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
		if(getLowestDistance(turns) == 0.)
			return turns;
		
//		ArrayList<ArrayList<Turn>> splittedTurns = new ArrayList<ArrayList<Turn>>();
//		System.out.println("turn Size: " + turns.size());
//		splittedTurns.add(listToArrayList(turns.subList(0, turns.size()/4)));
//		splittedTurns.add(listToArrayList(turns.subList(turns.size()/4, turns.size()/2)));
//		splittedTurns.add(listToArrayList(turns.subList(turns.size()/2, (turns.size()/2)+(turns.size()/4))));
//		splittedTurns.add(listToArrayList(turns.subList((turns.size()/2)+(turns.size()/4), turns.size()-1)));
//		
//		ArrayList<Turn> newTurns = new ArrayList<Turn>();
//		
//		Thread th = new Thread(() -> newTurns.addAll(iterateOverTurnsForSimulation(splittedTurns.get(0))));
//		Thread th2 = new Thread(() -> newTurns.addAll(iterateOverTurnsForSimulation(splittedTurns.get(1))));
//		Thread th3 = new Thread(() -> newTurns.addAll(iterateOverTurnsForSimulation(splittedTurns.get(2))));
//		Thread th4 = new Thread(() -> newTurns.addAll(iterateOverTurnsForSimulation(splittedTurns.get(3))));
//		
//		th.start();
//		th2.start();
//		th3.start();
//		th4.start();
//		
//		try {
//			th.join();
//			th2.join();
//			th3.join();
//			th4.join();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//		return newTurns;
		return iterateOverTurnsForSimulation(turns);
	}
	
	private ArrayList<Turn> iterateOverTurnsForSimulation(ArrayList<Turn> turns){
		HashMap<Turn, Turn> simulatedTurns = new HashMap<>();
		ArrayList<Turn> bestTurns = new ArrayList<Turn>();
		AwaitMoveMessageType awaitMoveMessage = new AwaitMoveMessageType();
		double distanceSecond = 0.1;
		
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
			if(turn.getRating() > secondTurn.getRating() && secondTurn.getRating() <= distanceSecond) {
				distanceSecond = secondTurn.getRating();
				simulatedTurns.put(secondTurn, turn);
			}
		}
		if(simulatedTurns.size() > 0) {
			Set<Turn> bestSecondTurns = simulatedTurns.keySet(); 
			Iterator<Turn> it = bestSecondTurns.iterator();
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
