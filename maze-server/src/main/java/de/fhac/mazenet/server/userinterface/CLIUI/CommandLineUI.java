package de.fhac.mazenet.server.userinterface.CLIUI;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Game;
import de.fhac.mazenet.server.Player;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.userinterface.UI;

import java.util.List;

/**
 * Featurepreview für eine CommandlineUI
 * <p>
 * Hauptsächlich als Vorbereitung für UnitTests/Automatisierte Tests
 * <p>
 * Alphastatus!!! Erwarte Fehler.
 */
public class CommandLineUI implements UI {

    private static CommandLineUI instance;
    private Game game;

    /**
     * privater Constructor wegen Singleton
     */
    private CommandLineUI() {
    }

    public static UI getInstance() {
        if (instance == null)
            instance = new CommandLineUI();
        return instance;
    }

    @Override
    public void displayMove(MoveMessageType moveMessage, Board board, long moveDelay, long shiftDelay, boolean treasureReached) {
        try {
            Thread.sleep(moveDelay);
            Thread.sleep(shiftDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePlayerStatistics(List<Player> statistics, Integer currentPlayerID) {
        for (Player player : statistics) {
            System.out.println(player);
        }
    }

    @Override
    public void init(Board board) {
        System.out.println("init");
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
        System.out.println("setGame");
        UserPrompt userPrompt = new UserPrompt(instance);
        new Thread(userPrompt).start();
    }

    @Override
    public void gameEnded(Player winner) {
        System.out.println("Gewinner ist " + winner.getName() + " (ID:" + winner.getID() + ")");
    }

    public void startGame() {
        if (game == null) {
            setGame(new Game());
        }
        game.parsArgs();
        game.setUserinterface(this);
        game.start();

    }

    public void stopGame() {
        if (game != null) {
            game.stopGame();
            game = null;
        }
        game = new Game();
    }

}
