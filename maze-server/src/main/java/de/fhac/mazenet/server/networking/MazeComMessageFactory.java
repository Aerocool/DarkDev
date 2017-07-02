package de.fhac.mazenet.server.networking;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Player;
import de.fhac.mazenet.server.generated.*;
import de.fhac.mazenet.server.generated.WinMessageType.Winner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MazeComMessageFactory {

    static private ObjectFactory objectFactory = new ObjectFactory();

    /**
     * privater Konstruktor wegen Factory
     */
    private MazeComMessageFactory() {
    }

    public static MazeCom createLoginReplyMessage(int newID) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.LOGINREPLY);
        mazeCom.setId(newID);
        mazeCom.setLoginReplyMessage(objectFactory.createLoginReplyMessageType());
        mazeCom.getLoginReplyMessage().setNewID(newID);
        return mazeCom;
    }

    public static MazeCom createAcceptMessage(int playerID, ErrorType error) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.ACCEPT);
        mazeCom.setId(playerID);
        mazeCom.setAcceptMessage(objectFactory.createAcceptMessageType());
        mazeCom.getAcceptMessage().setErrorCode(error);
        mazeCom.getAcceptMessage().setAccept(error == ErrorType.NOERROR);
        return mazeCom;
    }

    public static MazeCom createWinMessage(int playerID, int winnerId, String name, Board board) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.WIN);
        mazeCom.setId(playerID);
        mazeCom.setWinMessage(objectFactory.createWinMessageType());
        Winner winner = objectFactory.createWinMessageTypeWinner();
        winner.setId(winnerId);
        winner.setValue(name);
        mazeCom.getWinMessage().setWinner(winner);
        mazeCom.getWinMessage().setBoard(board);
        return mazeCom;
    }

    public static MazeCom createDisconnectMessage(int playerID, String name, ErrorType error) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.DISCONNECT);
        mazeCom.setId(playerID);
        mazeCom.setDisconnectMessage(objectFactory.createDisconnectMessageType());
        mazeCom.getDisconnectMessage().setErrorCode(error);
        mazeCom.getDisconnectMessage().setName(name);
        return mazeCom;
    }

    public static MazeCom createAwaitMoveMessage(HashMap<Integer, Player> players, Integer currentPlayerID, Board board,
            List<TreasureType> foundTreasures) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.AWAITMOVE);
        mazeCom.setId(players.get(currentPlayerID).getID());
        mazeCom.setAwaitMoveMessage(objectFactory.createAwaitMoveMessageType());
        // Brett uebergeben
        mazeCom.getAwaitMoveMessage().setBoard(board);
        mazeCom.getAwaitMoveMessage().setTreasure(players.get(currentPlayerID).getCurrentTreasure());
        mazeCom.getAwaitMoveMessage().getFoundTreasures().addAll(foundTreasures);
        List<Integer> sortedPlayers = new ArrayList<>(players.keySet());
        Collections.sort(sortedPlayers);
        for (Integer playerID : sortedPlayers) {
            TreasuresToGoType treasuresToGo = objectFactory.createTreasuresToGoType();
            treasuresToGo.setPlayer(playerID);
            treasuresToGo.setTreasures(players.get(playerID).treasuresToGo());
            mazeCom.getAwaitMoveMessage().getTreasuresToGo().add(treasuresToGo);
        }
        return mazeCom;
    }

    /**
     * convenience-Methode für Clients, wird vom Server nicht benötigt
     * 
     * @param name
     *            der gewählte Gruppenname
     * @return MazeCom-Nachricht mit beinhalteter LoginMessage
     */
    public static MazeCom createLoginMessage(String name) {
        MazeCom mazeCom = objectFactory.createMazeCom();
        mazeCom.setMcType(MazeComType.LOGIN);
        mazeCom.setId(-1);
        mazeCom.setLoginMessage(objectFactory.createLoginMessageType());
        mazeCom.getLoginMessage().setName(name);
        return mazeCom;
    }
}
