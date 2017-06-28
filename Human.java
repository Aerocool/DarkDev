package client;

import java.util.Scanner;

import de.fhaachen.mazenet.generated.AwaitMoveMessageType;
import de.fhaachen.mazenet.generated.MazeCom;
import de.fhaachen.mazenet.generated.MoveMessageType;
import de.fhaachen.mazenet.generated.TreasureType;
import de.fhaachen.mazenet.generated.TreasuresToGoType;

public class Human extends Actor {

	// user command finals
	private static final String INPUTSEP = ",";

	// cmd line scanner
	Scanner inputScanner;

	public Human(TCPClient_JW client, int playerID) {
		super(client, playerID);

		inputScanner = new Scanner(System.in);
	}

	public void printHelp() {
		System.out.println("Coordinates range from 0,0 (upper left corner) to 6,6 (lower right).");
		System.out.println("Notation is row,column. Separate with comma");
		System.out.println("Shift cards coordinates only can take values from {1,3,5}");
		System.out.println("Treasures are marked by numbers 1 to 24. For graphical correspondence, see:");
		System.out.println("mazenet-server/src/de/fhaachen/mazenet/server/userInterface/resources");
	}

	@Override
	public void run() {

		while (client.noWinnerYet()) {

			if (newMoveRequested) {

				// extract awm
				board = current_awm.getBoard();
				treasuresToGo = current_awm.getTreasuresToGo();
				foundTreasures = current_awm.getFoundTreasures();
				treasure = current_awm.getTreasure();

				/*
				 * Print game state information.
				 */
				System.out.println("\n\nPlayer " + id + ": Sie sind am Zug.");
				System.out.println("Verbleibende Ziele pro Spieler:");
				int tID = 0;
				for (TreasuresToGoType t : treasuresToGo) {
					System.out.printf("SpielerID %d: %d Ziele\n", tID, t.getTreasures());
				}
				System.out.println("Ihr aktuelles Ziel: " + getTreasureDescription(treasure));
				try {
					System.out.printf("Die verbotene ShiftCard-Position: %d,%d \n", board.getForbidden().getRow(),
							board.getForbidden().getCol());
				} catch (NullPointerException e) {
					System.out.println("(Spielstart: keine verbotene Shift-Card-Position.");
				}

				/*
				 * Get user input
				 */
				System.out.println("Bitte geben Sie die ShiftCard-Insert-Koordinate ein.");
				String[] cardPos = inputScanner.nextLine().split(INPUTSEP);
				System.out.println("Bitte geben Sie die Player-Koordinate an.");
				String[] pinPos = inputScanner.nextLine().split(INPUTSEP);

				MoveMessageType move = mcmfe.createMoveMessage(board.getShiftCard(), Integer.parseInt(cardPos[0]),
						Integer.parseInt(cardPos[1]), Integer.parseInt(pinPos[0]), Integer.parseInt(pinPos[1]));
				System.out.printf("MoveMessage created: shiftcardPos: %d,%d. Pinpos: %d,%d \n",
						move.getShiftPosition().getRow(), move.getShiftPosition().getCol(),
						move.getNewPinPos().getRow(), move.getNewPinPos().getCol());

				// alert client that new move can be sent
				client.setMoveToSend(move);

			} else {
				// wait for client to requestMove
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public String getTreasureDescription(TreasureType t) {
		String s = "";
		switch (t) {
		case SYM_01:
			s = "Teilen/Share-Zeichen";
			break;
		case SYM_02:
			s = "WLAN-Signal";
			break;
		case SYM_03:
			s = "Blauer Keil auf orangem Hintergrund";
			break;
		case SYM_04:
			s = "Chrome";
			break;
		case SYM_05:
			s = "Welt";
			break;
		case SYM_06:
			s = "RSS";
			break;
		case SYM_07:
			s = "LAN-Kabelanschluss";
			break;
		case SYM_08:
			s = "Monitor";
			break;
		case SYM_09:
			s = "Java Cup";
			break;
		case SYM_10:
			s = "HTML Tag";
			break;
		case SYM_11:
			s = "Google g";
			break;
		case SYM_12:
			s = "Ubuntu Logo";
			break;
		case SYM_13:
			s = "Windows Logo";
			break;
		case SYM_14:
			s = "Git Logo (Weisse Kantenknoten auf rot)";
			break;
		case SYM_15:
			s = "Hellblaues Diagnosefenster";
			break;
		case SYM_16:
			s = "Youtube";
			break;
		case SYM_17:
			s = "Debian (Spirale)";
			break;
		case SYM_18:
			s = "Facebook";
			break;
		case SYM_19:
			s = "Android";
			break;
		case SYM_20:
			s = "Google Drive (Festplatte)";
			break;
		case SYM_21:
			s = "Reload / Recycle - Pfeile";
			break;
		case SYM_22:
			s = "USB-Anschluss-Logo";
			break;
		case SYM_23:
			s = "Bluetooth";
			break;
		case SYM_24:
			s = "Drucker";
			break;
		default:
			s = "Not a treasure!";
			break;
		}
		return s;
	}

}
