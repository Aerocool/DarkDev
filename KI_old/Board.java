package client;

import de.fhaachen.mazenet.generated.BoardType;
import de.fhaachen.mazenet.generated.CardType;
import de.fhaachen.mazenet.generated.MoveMessageType;
import de.fhaachen.mazenet.generated.PositionType;
import de.fhaachen.mazenet.generated.TreasureType;

import de.fhaachen.mazenet.generated.CardType.Openings;
import de.fhaachen.mazenet.generated.CardType.Pin;

import java.util.ArrayList;
import java.util.List;

public class Board extends BoardType {


    public CardType getCard(int row, int col) {
		return this.getRow().get(row).getCol().get(col);
	}
    
    
    
    
    
    public PositionType findPlayer(Integer PlayerID) {
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 7; j++) {
				Pin pinsOnCard = getCard(i, j).getPin();
				for (Integer pin : pinsOnCard.getPlayerID()) {
					if (pin == PlayerID) {
						PositionType pos = new PositionType();
						pos.setCol(j);
						pos.setRow(i);
						return pos;
					}
				}
			}
		}
		// Pin nicht gefunden.
		// XXX: Darf eigentlich nicht vorkommen
		return null;
	}
    
    public List<PositionType> getAllReachablePositions(PositionType position) {
		List<PositionType> erreichbarePositionen = new ArrayList<PositionType>();
		int[][] erreichbar = new int[7][7];
		erreichbar[position.getRow()][position.getCol()] = 1;
		erreichbar = getAllReachablePositionsMatrix(position, erreichbar);
		for (int i = 0; i < erreichbar.length; i++) {
			for (int j = 0; j < erreichbar[0].length; j++) {
				if (erreichbar[i][j] == 1) {
					erreichbarePositionen.add(new Position(i, j));
				}
			}
		}
		return erreichbarePositionen;
	}
    
    private int[][] getAllReachablePositionsMatrix(PositionType position,
			int[][] erreichbar) {
		for (PositionType p1 : getDirectReachablePositions(position)) {
			if (erreichbar[p1.getRow()][p1.getCol()] == 0) {
				erreichbar[p1.getRow()][p1.getCol()] = 1;
				getAllReachablePositionsMatrix(p1, erreichbar);
			}
		}
		return erreichbar;
	}
    
    public boolean pathPossible(PositionType oldPos, PositionType newPos) {
		if (oldPos == null || newPos == null)
			return false;
		Position oldP = new Position(oldPos);
		Position newP = new Position(newPos);
		return getAllReachablePositions(oldP).contains(newP);
	}
    

	private List<PositionType> getDirectReachablePositions(PositionType position) {
		List<PositionType> positionen = new ArrayList<PositionType>();
		CardType k = this.getCard(position.getRow(), position.getCol());
		Openings openings = k.getOpenings();
		if (openings.isLeft()) {
			if (position.getCol() - 1 >= 0
					&& getCard(position.getRow(), position.getCol() - 1)
							.getOpenings().isRight()) {
				positionen.add(new Position(position.getRow(), position
						.getCol() - 1));
			}
		}
		if (openings.isTop()) {
			if (position.getRow() - 1 >= 0
					&& getCard(position.getRow() - 1, position.getCol())
							.getOpenings().isBottom()) {
				positionen.add(new Position(position.getRow() - 1, position
						.getCol()));
			}
		}
		if (openings.isRight()) {
			if (position.getCol() + 1 <= 6
					&& getCard(position.getRow(), position.getCol() + 1)
							.getOpenings().isLeft()) {
				positionen.add(new Position(position.getRow(), position
						.getCol() + 1));
			}
		}
		if (openings.isBottom()) {
			if (position.getRow() + 1 <= 6
					&& getCard(position.getRow() + 1, position.getCol())
							.getOpenings().isTop()) {
				positionen.add(new Position(position.getRow() + 1, position
						.getCol()));
			}
		}
		return positionen;
	}
    
 // Fuehrt nur das Hereinschieben der Karte aus!!!
 	public void proceedShift(MoveMessageType move) {
 		Position sm = new Position(move.getShiftPosition());
 		if (sm.getCol() % 6 == 0) { // Col=6 oder 0
 			if (sm.getRow() % 2 == 1) {
 				// horizontal schieben
 				int row = sm.getRow();
 				int start = (sm.getCol() + 6) % 12; // Karte die rausgenommen
 													// wird
 				setShiftCard(getCard(row, start));

 				if (start == 6) {
 					for (int i = 6; i > 0; --i) {
 						setCard(row, i, new Card(getCard(row, i - 1)));
 					}
 				} else {// Start==0
 					for (int i = 0; i < 6; ++i) {
 						setCard(row, i, new Card(getCard(row, i + 1)));
 					}
 				}
 			}
 		} else if (sm.getRow() % 6 == 0) {
 			if (sm.getCol() % 2 == 1) {
 				// vertikal schieben
 				int col = sm.getCol();
 				int start = (sm.getRow() + 6) % 12; // Karte die rausgenommen
 													// wird
 				setShiftCard(getCard(start, col));
 				if (start == 6) {
 					for (int i = 6; i > 0; --i) {
 						setCard(i, col, new Card(getCard(i - 1, col)));
 					}
 				} else {// Start==0
 					for (int i = 0; i < 6; ++i) {
 						setCard(i, col, new Card(getCard(i + 1, col)));
 					}
 				}

 			}
 		}
 		forbidden = sm.getOpposite();
 		Card c = null;
 		c = new Card(move.getShiftCard());
 		// Wenn Spielfigur auf neuer shiftcard steht,
 		// muss dieser wieder aufs Brett gesetzt werden
 		// Dazu wird Sie auf die gerade hereingeschoben
 		// Karte gesetzt
 		if (!shiftCard.getPin().getPlayerID().isEmpty()) {
 			// Figur zwischenspeichern
 			Pin temp = shiftCard.getPin();
 			// Figur auf SchiebeKarte löschen
 			shiftCard.setPin(new Pin());
 			// Zwischengespeicherte Figut auf
 			// neuer Karte plazieren
 			c.setPin(temp);
 		}
 		setCard(sm.getRow(), sm.getCol(), c);
 	}

 	
 	public Board fakeShift(MoveMessageType move) throws CloneNotSupportedException {
		Board fake = (Board) this.clone();
		fake.proceedShift(move);
		return fake;
	}
 	
 	public void setCard(int row, int col, Card c) {
		// Muss ueberschrieben werden, daher zuerst entfernen und dann...
		this.getRow().get(row).getCol().remove(col);
		// ...hinzufuegen
		this.getRow().get(row).getCol().add(col, c);
	}
    
    public PositionType findTreasure(TreasureType treasureType) {
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 7; j++) {
				TreasureType treasure = getCard(i, j).getTreasure();
				if (treasure == treasureType) {
					PositionType pos = new PositionType();
					pos.setCol(j);
					pos.setRow(i);
					return pos;
				}
			}
		}
		// Schatz nicht gefunden, kann nur bedeuten, dass Schatz sich auf
		// Schiebekarte befindet
		return null;
	}
}


