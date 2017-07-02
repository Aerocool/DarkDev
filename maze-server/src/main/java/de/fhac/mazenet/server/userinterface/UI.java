package de.fhac.mazenet.server.userinterface;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Game;
import de.fhac.mazenet.server.Player;
import de.fhac.mazenet.server.generated.MoveMessageType;

import java.util.List;

public interface UI {
    public void displayMove(MoveMessageType moveMessage, Board board, long moveDelay,
                            long shiftDelay, boolean treasureReached);

    public void updatePlayerStatistics(List<Player> statistics, Integer currentPlayerID);

    public void init(Board board);

    public void setGame(Game game);

    public void gameEnded(Player winner);
}
