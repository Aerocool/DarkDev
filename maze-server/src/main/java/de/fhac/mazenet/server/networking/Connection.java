/*
 * Connection regelt die serverseitige Protokollarbeit
 */
package de.fhac.mazenet.server.networking;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Game;
import de.fhac.mazenet.server.Player;
import de.fhac.mazenet.server.config.Settings;
import de.fhac.mazenet.server.generated.*;
import de.fhac.mazenet.server.timeouts.TimeOutManager;
import de.fhac.mazenet.server.tools.Debug;
import de.fhac.mazenet.server.tools.DebugLevel;

import javax.xml.bind.UnmarshalException;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Connection {

    private Socket socket;
    private Player player;
    private XmlInStream inFromClient;
    private XmlOutStream outToClient;
    private TimeOutManager timeOutManager;
    private Game currentGame;

    /**
     * Speicherung des Sockets und oeffnen der Streams
     * 
     * @param socket
     *            Socket der Verbindung
     */
    public Connection(Socket socket, Game game, int newId) {
        // TODO entfernen => nur fuer Test
        this.socket = socket;
        this.currentGame = game;
        try {
            this.inFromClient = new XmlInStream(this.socket.getInputStream());
        } catch (IOException e) {
            System.err.println(Messages.getString("Connection.couldNotOpenInputStream")); //$NON-NLS-1$
        }
        try {
            this.outToClient = new XmlOutStream(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println(Messages.getString("Connection.couldNotOpenOutputStream")); //$NON-NLS-1$
        }
        this.player = new Player(newId, this);
        this.timeOutManager = new TimeOutManager();

    }

    /**
     * Allgemeines senden einer fertigen MazeCom-Instanz
     */
    public void sendMessage(MazeCom mazeCom, boolean withTimer) {
        // Timer starten, der beim lesen beendet wird
        // Ablauf Timer = Problem User
        if (withTimer)
            this.timeOutManager.startSendMessageTimeOut(this.player.getID(), this);
        this.outToClient.write(mazeCom);
    }

    /**
     * Allgemeines empfangen einer MazeCom-Instanz
     * 
     * @return
     */
    public MazeCom receiveMessage() {
        MazeCom result = null;

        try {
            result = this.inFromClient.readMazeCom();
        } catch (UnmarshalException e) {
            Throwable xmle = e.getLinkedException();
            Debug.print(Messages.getString("Connection.XmlError") + xmle.getMessage(), DebugLevel.DEFAULT); //$NON-NLS-1$
        } catch (IOException e) {
            Debug.print(Messages.getString("XmlInStream.errorReadingMessage"), //$NON-NLS-1$
                    DebugLevel.DEFAULT);
            Debug.print(Messages.getString("Connection.playerExitedUnexpected"), //$NON-NLS-1$
                    DebugLevel.DEFAULT);
            // entfernen des Spielers
            this.currentGame.removePlayer(this.player.getID());
        }
        this.timeOutManager.stopSendMessageTimeOut(this.player.getID());
        return result;
    }

    /**
     * Allgemeines erwarten eines Login
     * 
     * @param newId
     * @return Neuer Player, bei einem Fehler jedoch null
     */
    public Player login(int newId, Stack<Integer> availablePlayers) {
        this.player = new Player(newId, this);
        LoginThread lt = new LoginThread(this, this.player, availablePlayers);
        lt.start();
        return this.player;
    }

    /**
     * Anfrage eines Zuges beim Spieler
     * 
     * @param board
     *            aktuelles Spielbrett
     * @return Valieder Zug des Spielers oder NULL
     */
    public MoveMessageType awaitMove(HashMap<Integer, Player> players, Board board, int tries,
            List<TreasureType> foundTreasures) {
        if (players.get(player.getID()) != null && tries < Settings.MOVETRIES) {
            sendMessage(MazeComMessageFactory.createAwaitMoveMessage(players, player.getID(), board, foundTreasures),
                    true);
            MazeCom result = this.receiveMessage();
            if (result != null && result.getMcType() == MazeComType.MOVE) {
                if (currentGame.getBoard().validateTransition(result.getMoveMessage(), player.getID())) {
                    sendMessage(MazeComMessageFactory.createAcceptMessage(player.getID(), ErrorType.NOERROR), false);
                    return result.getMoveMessage();
                }
                // nicht Regelkonform
                sendMessage(MazeComMessageFactory.createAcceptMessage(player.getID(), ErrorType.ILLEGAL_MOVE), false);
                return awaitMove(players, board, ++tries, foundTreasures);
            }
            // XML nicht verwertbar
            sendMessage(MazeComMessageFactory.createAcceptMessage(player.getID(), ErrorType.AWAIT_MOVE), false);
            return awaitMove(players, board, ++tries, foundTreasures);

        }
        disconnect(ErrorType.TOO_MANY_TRIES);

        return null;
    }

    @Deprecated
    public MoveMessageType awaitMove3(HashMap<Integer, Player> spieler, Board board, int tries,
            List<TreasureType> foundTreasures) {
        this.sendMessage(
                MazeComMessageFactory.createAwaitMoveMessage(spieler, this.player.getID(), board, foundTreasures),
                true);
        MazeCom result = this.receiveMessage();
        if (result != null && result.getMcType() == MazeComType.MOVE) {
            // Antwort mit NOERROR
            if (this.currentGame.getBoard().validateTransition(result.getMoveMessage(), this.player.getID())) {
                this.sendMessage(MazeComMessageFactory.createAcceptMessage(this.player.getID(), ErrorType.NOERROR),
                        false);
                return result.getMoveMessage();
            } else if (tries < Settings.MOVETRIES)
                return illegalMove(spieler, board, ++tries, foundTreasures);
            else {
                disconnect(ErrorType.TOO_MANY_TRIES);
                return null;
            }

        }
        // else nicht benoetigt wegen return-Statements im then
        this.sendMessage(MazeComMessageFactory.createAcceptMessage(this.player.getID(), ErrorType.AWAIT_MOVE), false);

        if (tries < Settings.MOVETRIES)
            return awaitMove3(spieler, board, ++tries, foundTreasures);

        disconnect(ErrorType.TOO_MANY_TRIES);
        return null;
    }

    /**
     * Erhaltener Move ist falsch gewesen => Fehler senden und neuen AwaitMove
     * sende!
     * 
     * @param brett
     *            aktuelles Spielbrett
     * @return Zug des Spielers
     */
    @Deprecated
    public MoveMessageType illegalMove(HashMap<Integer, Player> spieler, Board brett, int tries,
            List<TreasureType> foundTreasures) {
        this.sendMessage(MazeComMessageFactory.createAcceptMessage(this.player.getID(), ErrorType.ILLEGAL_MOVE), false);
        if (tries < Settings.MOVETRIES)
            return this.awaitMove3(spieler, brett, tries, foundTreasures);
        disconnect(ErrorType.TOO_MANY_TRIES);
        return null;
    }

    /**
     * sendet dem Spieler den Namen des Gewinners sowie dessen ID und das
     * Schlussbrett
     * 
     * @param winnerId
     * @param name
     * @param board
     */
    public void sendWin(int winnerId, String name, Board board) {
        this.sendMessage(MazeComMessageFactory.createWinMessage(this.player.getID(), winnerId, name, board), false);
        try {
            this.inFromClient.close();
            this.outToClient.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Senden, dass Spieler diconnected wurde
     */
    public void disconnect(ErrorType error) {
        this.sendMessage(
                MazeComMessageFactory.createDisconnectMessage(this.player.getID(), this.player.getName(), error),
                false);
        try {
            this.inFromClient.close();
            this.outToClient.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // entfernen des Spielers
        this.currentGame.removePlayer(this.player.getID());
    }
}
