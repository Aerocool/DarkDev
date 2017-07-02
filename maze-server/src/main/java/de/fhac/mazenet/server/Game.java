package de.fhac.mazenet.server;

import de.fhac.mazenet.server.config.Settings;
import de.fhac.mazenet.server.generated.ErrorType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.TreasureType;
import de.fhac.mazenet.server.networking.Connection;
import de.fhac.mazenet.server.networking.TCPConnectionCreationTask;
import de.fhac.mazenet.server.timeouts.TimeOutManager;
import de.fhac.mazenet.server.tools.Debug;
import de.fhac.mazenet.server.tools.DebugLevel;
import de.fhac.mazenet.server.userinterface.UI;
import org.apache.commons.cli.*;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Game extends Thread {

    /**
     * beinhaltet die Spieler, die mit dem Server verbunden sind und die durch
     * die ID zugreifbar sind
     */
    private HashMap<Integer, Player> spieler;
    private ServerSocket serverSocket;
    private ServerSocket sslServerSocket;
    private TimeOutManager timeOutManager;
    private Board spielBrett;
    /**
     * Defaultwert -1, solange kein Gewinner feststeht
     */
    private Integer winner = -1;
    private UI userinterface;
    private List<TreasureType> foundTreasures;
    private String[] args;
    private boolean ssl;

    public Game() {
        winner = -1;
        spieler = new HashMap<Integer, Player>();
        timeOutManager = new TimeOutManager();
        foundTreasures = new ArrayList<TreasureType>();
    }

    public static void main(String[] args) {
        Game currentGame = new Game();
        currentGame.args = args;
        currentGame.parsArgs();
        Locale.setDefault(Settings.LOCALE);
        currentGame.userinterface = Settings.USERINTERFACE;
        currentGame.userinterface.init(new Board());
        currentGame.userinterface.setGame(currentGame);
    }

    /**
     * Auf TCP Verbindungen warten und den Spielern die Verbindung ermoeglichen
     */
    public void init(int playerCount) {
        Debug.addDebugger(System.out, Settings.DEBUGLEVEL);
        Debug.print(Messages.getString("Game.initFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        // Socketinitialisierung aus dem Constructor in init verschoben. Sonst
        // Errors wegen Thread.
        // init wird von run (also vom Thread) aufgerufen, im Gegesatz zum
        // Constructor
        try {

            // unencrypted
            serverSocket = new ServerSocket(Settings.PORT);
            ssl = false;
            if (Settings.SSL_CERT_STORE != "") {
                // encrypted

                // Setup SSL
                if (new File(Settings.SSL_CERT_STORE).exists()) {
                    System.setProperty("javax.net.ssl.keyStorePassword", Settings.SSL_CERT_STORE_PASSWD);
                    System.setProperty("javax.net.ssl.keyStore", Settings.SSL_CERT_STORE);
                    sslServerSocket = SSLServerSocketFactory.getDefault().createServerSocket(Settings.SSL_PORT);
                    ssl = true;
                } else {
                    Debug.print(Messages.getString("Game.certStoreNotFound"), DebugLevel.DEFAULT);
                }
            } else {
                Debug.print(Messages.getString("Game.noSSL"), DebugLevel.DEFAULT);
            }
        } catch (IOException e) {
            // FIXME differentiate between SSL Error und Port used error
            System.err.println(e.getLocalizedMessage());
            Debug.print(Messages.getString("Game.portUsed"), DebugLevel.DEFAULT); //$NON-NLS-1$
        }
        timeOutManager.startLoginTimeOut(this);
        Stack<Integer> availableIds = new Stack<>();
        List<String> connectedIPs = new ArrayList<>();

        for (int i = 1; i <= playerCount; i++) {
            availableIds.push(i);
        }
        if (!Settings.TESTBOARD)
            Collections.shuffle(availableIds);
        // preparing Tasks for SSL and unencrypted
        final CyclicBarrier barrier = new CyclicBarrier(2);
        Socket mazeClient = null;
        TCPConnectionCreationTask waitForConnectionTask = new TCPConnectionCreationTask(serverSocket, barrier);
        TCPConnectionCreationTask waitForSSLConnectionTask = new TCPConnectionCreationTask(sslServerSocket, barrier);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> noSSLStatus = null;
        Future<?> SSLStatus = null;

        // Warten bis die Initialisierung durchgelaufen ist
        boolean spielbereit = false;
        while (!spielbereit) {
            // FIXME: wenn sich kein Spieler verbindet, sollte ewig gewartet
            // werden
            while (!availableIds.isEmpty()) {
                try {
                    int id = availableIds.pop();
                    Debug.print(Messages.getString("Game.waitingForPlayer") + " (" //$NON-NLS-1$ //$NON-NLS-2$
                            + (playerCount - availableIds.size()) + "/" + playerCount //$NON-NLS-1$
                            + ")", DebugLevel.DEFAULT); //$NON-NLS-1$
                    // Neustart des benutzen serverSockets
                    if (noSSLStatus == null || noSSLStatus.isDone()) {
                        noSSLStatus = pool.submit(waitForConnectionTask);
                    }
                    if (ssl && (SSLStatus == null || SSLStatus.isDone())) {
                        SSLStatus = pool.submit(waitForSSLConnectionTask);
                    }
                    barrier.await();
                    //Abrufen des Sockets für die neue Verbindung
                    if (noSSLStatus.isDone()) {
                        mazeClient = waitForConnectionTask.getIncoming();
                    }
                    // SSLStatus.isDone() liefert bei nicht configuriertem SSL auch true zurück
                    // TODO Besseren weg finden zu erkennen ob SSL configuriert ist
                    // IDEE: Checken ob Settings.SSL_CERT_STORE-Datei existiert
                    if (ssl && SSLStatus.isDone()) {
                        mazeClient = waitForSSLConnectionTask.getIncoming();
                    }
                    // Nur ein Verbindung pro IP erlauben (Ausnahme localhost)
                    InetAddress inetAddress = mazeClient.getInetAddress();
                    String ip = inetAddress.getHostAddress();
                    if (!connectedIPs.contains(ip)) {
                        if (!inetAddress.isLoopbackAddress()) {
                            connectedIPs.add(ip);
                        }
                        Connection c = new Connection(mazeClient, this, id);
                        spieler.put(id, c.login(id, availableIds));
                    } else {
                        Debug.print(String.format(Messages.getString("Game.HostAlreadyConnected"), ip), //$NON-NLS-1$
                                DebugLevel.DEFAULT);
                    }
                } catch (InterruptedException e) {
                    Debug.print(Messages.getString("Game.playerWaitingTimedOut"), //$NON-NLS-1$
                            DebugLevel.DEFAULT);

                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
            spielbereit = true;
            for (Integer id : spieler.keySet()) {
                Player p = spieler.get(id);
                if (!p.isInitialized()) {
                    spielbereit = false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        pool.shutdownNow();
        timeOutManager.stopLoginTimeOut();
        // Spielbrett generieren
        spielBrett = new Board();
        // Verteilen der Schatzkarten
        ArrayList<TreasureType> treasureCardPile = new ArrayList<>();
        treasureCardPile.add(TreasureType.SYM_01);
        treasureCardPile.add(TreasureType.SYM_02);
        treasureCardPile.add(TreasureType.SYM_03);
        treasureCardPile.add(TreasureType.SYM_04);
        treasureCardPile.add(TreasureType.SYM_05);
        treasureCardPile.add(TreasureType.SYM_06);
        treasureCardPile.add(TreasureType.SYM_07);
        treasureCardPile.add(TreasureType.SYM_08);
        treasureCardPile.add(TreasureType.SYM_09);
        treasureCardPile.add(TreasureType.SYM_10);
        treasureCardPile.add(TreasureType.SYM_11);
        treasureCardPile.add(TreasureType.SYM_12);
        treasureCardPile.add(TreasureType.SYM_13);
        treasureCardPile.add(TreasureType.SYM_14);
        treasureCardPile.add(TreasureType.SYM_15);
        treasureCardPile.add(TreasureType.SYM_16);
        treasureCardPile.add(TreasureType.SYM_17);
        treasureCardPile.add(TreasureType.SYM_18);
        treasureCardPile.add(TreasureType.SYM_19);
        treasureCardPile.add(TreasureType.SYM_20);
        treasureCardPile.add(TreasureType.SYM_21);
        treasureCardPile.add(TreasureType.SYM_22);
        treasureCardPile.add(TreasureType.SYM_23);
        treasureCardPile.add(TreasureType.SYM_24);
        if (!Settings.TESTBOARD)
            Collections.shuffle(treasureCardPile);
        if (spieler.size() == 0) {
            System.err.println(Messages.getString("Game.noPlayersConnected")); //$NON-NLS-1$
            stopGame();
            return;
        }
        int anzCards = treasureCardPile.size() / spieler.size();
        int i = 0;
        for (Integer player : spieler.keySet()) {
            ArrayList<TreasureType> cardsPerPlayer = new ArrayList<>();
            for (int j = i * anzCards; j < (i + 1) * anzCards; j++) {
                cardsPerPlayer.add(treasureCardPile.get(j));
            }
            spieler.get(player).setTreasure(cardsPerPlayer);
            ++i;
        }

    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (sslServerSocket != null) {
                sslServerSocket.close();
            }
            if (sslServerSocket == null && serverSocket == null)
                Debug.print(Messages.getString("Game.serverSocketNull"), DebugLevel.DEFAULT); //$NON-NLS-1$
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private List<Player> playerToList() {
        Debug.print(Messages.getString("Game.playerToListFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        return new ArrayList<>(spieler.values());
    }

    private void singleTurn(Integer currPlayer) {
        /**
         * Connection.awaitMove checken -> Bei Fehler illegalMove -> liefert
         * neuen Zug
         */
        Debug.print(Messages.getString("Game.singleTurnFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        userinterface.updatePlayerStatistics(playerToList(), currPlayer);
        TreasureType t = spieler.get(currPlayer).getCurrentTreasure();
        spielBrett.setTreasure(t);
        Debug.print(Messages.getString("Game.boardBeforeMoveFromPlayerWithID") + currPlayer, //$NON-NLS-1$
                DebugLevel.VERBOSE);
        Debug.print(spielBrett.toString(), DebugLevel.DEBUG);
        MoveMessageType move = spieler.get(currPlayer).getConToClient().awaitMove(spieler, this.spielBrett, 0,
                foundTreasures);
        boolean found = false;
        if (move != null) {
            // proceedTurn gibt zurueck ob der Spieler seinen Schatz erreicht
            // hat
            if (spielBrett.proceedTurn(move, currPlayer)) {
                found = true;
                Debug.print(String.format(Messages.getString("Game.foundTreasure"), spieler.get(currPlayer).getName(), //$NON-NLS-1$
                        currPlayer), DebugLevel.DEFAULT);
                foundTreasures.add(t);
                // foundTreasure gibt zurueck wieviele
                // Schaetze noch zu finden sind
                if (spieler.get(currPlayer).foundTreasure() == 0) {
                    winner = currPlayer;
                }
            }
            userinterface.displayMove(move, spielBrett, Settings.MOVEDELAY, Settings.SHIFTDELAY, found);
        } else {
            Debug.print(Messages.getString("Game.gotNoMove"), DebugLevel.DEFAULT); //$NON-NLS-1$
        }
    }

    public Board getBoard() {
        return spielBrett;
    }

    /**
     * Aufraeumen nach einem Spiel
     */
    private void cleanUp() {
        Debug.print(Messages.getString("Game.cleanUpFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        if (winner > 0) {
            for (Integer playerID : spieler.keySet()) {
                Player s = spieler.get(playerID);
                s.getConToClient().sendWin(winner, spieler.get(winner).getName(), spielBrett);
            }
            userinterface.updatePlayerStatistics(playerToList(), winner);
            Debug.print(String.format(Messages.getString("Game.playerIDwon"), spieler //$NON-NLS-1$
                    .get(winner).getName(), winner), DebugLevel.DEFAULT);

        } else {
            while (spieler.size() > 0) {
                Player s = spieler.get(spieler.keySet().iterator().next());
                s.getConToClient().disconnect(ErrorType.NOERROR);

            }
        }
        stopGame();
    }

    public boolean somebodyWon() {
        return winner != -1;

    }

    public void setUserinterface(UI userinterface) {
        this.userinterface = userinterface;
    }

    public void parsArgs() {
        Options availableOptions = new Options();
        availableOptions.addOption("c", true, "path to property file for configuration");
        availableOptions.addOption("h", false, "displays this help message");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(availableOptions, args);
            if (cmd.hasOption("h")) {
                printCMDHelp(0, availableOptions);
            }
            String configPath = cmd.getOptionValue("c");
            // Wenn mit null aufgerufen, werden standardwerte benutzt
            Settings.reload(configPath);
        } catch (ParseException e) {
            printCMDHelp(1, availableOptions);
        }
    }

    private void printCMDHelp(int exitCode, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar maze-server.jar [options]\nAvailable Options:", options);
        System.exit(exitCode);
    }

    public void run() {
        Debug.print(Messages.getString("Game.runFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        Debug.print(Messages.getString("Game.startNewGame"), DebugLevel.DEFAULT); //$NON-NLS-1$
        // TODO Configfile austauschbar machen
        init(Settings.NUMBER_OF_PLAYERS);
        if (spieler.isEmpty()) {
            cleanUp();
            return;
        }
        userinterface.init(spielBrett);
        Integer currPlayer = nextPlayer(0);
        userinterface.updatePlayerStatistics(playerToList(), currPlayer);
        while (!somebodyWon()) {
            Debug.print(String.format(Messages.getString("Game.playersTurn"), spieler.get(currPlayer).getName(), //$NON-NLS-1$
                    currPlayer), DebugLevel.DEFAULT);
            singleTurn(currPlayer);
            try {
                currPlayer = nextPlayer(currPlayer);
            } catch (NoSuchElementException e) {
                Debug.print(Messages.getString("Game.AllPlayersLeft"), DebugLevel.DEFAULT); //$NON-NLS-1$
                stopGame();
            }
        }
        cleanUp();
    }

    private Integer nextPlayer(Integer currentPlayerID) throws NoSuchElementException {
        Debug.print(Messages.getString("Game.nextPlayerFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        //Iterator<Integer> idIterator = spieler.keySet().iterator();
        List<Integer> sortedPlayers = new ArrayList<>(spieler.keySet());
        Collections.sort(sortedPlayers);
        Iterator<Integer> idIterator = sortedPlayers.iterator();
        while (idIterator.hasNext()) {
            Integer id = idIterator.next();
            if (id.equals(currentPlayerID)) {
                break;
            }
        }
        if (idIterator.hasNext()) {
            return idIterator.next();
        }
        // Erste ID zurueckgeben,
        return spieler.keySet().iterator().next();
    }

    public void removePlayer(int id) {
        Debug.print(Messages.getString("Game.removePlayerFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        this.spieler.remove(id);
        Debug.print(String.format(Messages.getString("Game.playerIDleftGame"), id), //$NON-NLS-1$
                DebugLevel.DEFAULT);
    }

    public void stopGame() {
        Debug.print(Messages.getString("Game.stopFkt"), DebugLevel.VERBOSE); //$NON-NLS-1$
        Debug.print(Messages.getString("Game.stopGame"), DebugLevel.DEFAULT); //$NON-NLS-1$
        userinterface.gameEnded(spieler.get(winner));
        winner = -2;
        userinterface.setGame(null);
        timeOutManager.cancel();
        closeServerSocket();
    }
}
