package de.fhac.mazenet.server.userinterface.mazeFX;
/**
 * Created by Richard Zameitat on 25.05.2016.
 */

import de.fhac.mazenet.server.*;
import de.fhac.mazenet.server.config.Settings;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.userinterface.UI;
import de.fhac.mazenet.server.userinterface.mazeFX.animations.AddTransition;
import de.fhac.mazenet.server.userinterface.mazeFX.animations.AnimationFactory;
import de.fhac.mazenet.server.userinterface.mazeFX.data.Translate3D;
import de.fhac.mazenet.server.userinterface.mazeFX.data.VectorInt2;
import de.fhac.mazenet.server.userinterface.mazeFX.objects.CardFX;
import de.fhac.mazenet.server.userinterface.mazeFX.objects.PlayerFX;
import de.fhac.mazenet.server.userinterface.mazeFX.util.FakeTranslateBinding;
import de.fhac.mazenet.server.userinterface.mazeFX.util.MoveStateCalculator;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class MazeFX extends Application implements UI {

    private static final int BOARD_WIDTH = 7;
    private static final int BOARD_HEIGHT = 7;
    // private static final Translate3D SHIFT_CARD_TRANSLATE = new
    // Translate3D(0,-0.2,-(BOARD_HEIGHT/2.+1));
    private static final Translate3D SHIFT_CARD_TRANSLATE = new Translate3D(0, -3.3, 0);
    private static final double CAM_ROTATE_X_INITIAL = -50;
    private static final double CAM_ROTATE_Y_INITIAL = 0;
    private static MazeFX instance;
    // DIRTY HACK FOR GETTING AN INSTANCE STARTS HERE
    // (JavaFX ist not very good at creating instances ...)
    private static CountDownLatch instanceCreated = new CountDownLatch(1);
    // END OF HACK
    private static MazeFX lastInstance = null;
    private Game game;
    private Board board;
    private Stage primaryStage;
    private Parent root;
    private C_MainUI controller;
    private Group scene3dRoot;
    private CardFX[][] boardCards;
    private CardFX shiftCard;
    private Map<Integer, PlayerFX> players;
    private Map<Integer, PlayerStatFX> playerStats = new HashMap<>();
    private PlayerFX currentPlayer;
    private Rotate camRotateX = new Rotate(CAM_ROTATE_X_INITIAL, Rotate.X_AXIS);
    private Rotate camRotateY = new Rotate(CAM_ROTATE_Y_INITIAL, Rotate.Y_AXIS);

    public MazeFX() {
    }

    public static MazeFX getInstance() {
        if (instance == null)
            instance = newInstance();
        return instance;
    }

    private synchronized static MazeFX newInstance() {
        new Thread(() -> Application.launch(MazeFX.class)).start();
        try {
            instanceCreated.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        MazeFX instance = lastInstance;
        lastInstance = null;
        instanceCreated = new CountDownLatch(1);
        return instance;
    }

    public static Translate3D getCardTranslateForPosition(int x, int z) {
        final double midX = BOARD_WIDTH / 2.;
        final double midZ = BOARD_HEIGHT / 2.;
        final double offX = 0.5;
        final double offZ = -0.5;
        double newX = (x - midX) * 1 + offX;
        double newZ = (midZ - z) * 1 + offZ;
        return new Translate3D(newX, 0, newZ);
    }

    private static Translate3D getCardShiftBy(PositionType posT) {
        if (posT.getCol() == 0)
            return new Translate3D(1, 0, 0);
        if (posT.getCol() == BOARD_WIDTH - 1)
            return new Translate3D(-1, 0, 0);

        if (posT.getRow() == 0)
            return new Translate3D(0, 0, -1);
        if (posT.getRow() == BOARD_HEIGHT - 1)
            return new Translate3D(0, 0, 1);

        return new Translate3D(0, 0, 0); // no idea!
    }

    private static Translate3D getCardTranslateForShiftStart(PositionType posT) {
        return getCardTranslateForPosition(posT.getCol(), posT.getRow()).translate(getCardShiftBy(posT).invert());
    }

    private void instanceReady() {
        lastInstance = this;
        instanceCreated.countDown();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        primaryStage.onCloseRequestProperty().setValue(e -> {
            System.exit(0);
        });
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/layouts/MainUI.fxml")); //$NON-NLS-1$
        fxmlLoader.setResources(ResourceBundle.getBundle("locale")); //$NON-NLS-1$
        root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        init3dStuff();

        controller.addStartServerListener(this::startActionPerformed);
        controller.addStopServerListener(this::stopActionPerformed);

        primaryStage.setTitle(Messages.getString("MazeFX.WindowTitle")); //$NON-NLS-1$
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/maze.png")));
        primaryStage.show();
        instanceReady();
    }

    private PlayerStatFX createPlayerStat(int teamId) throws IOException {
        return new PlayerStatFX(teamId, board);
    }

    private void updatePlayerStats(List<Player> stats, Integer current) {
        currentPlayer = players.get(current);
        stats.forEach(p -> {
            try {
                PlayerStatFX stat = playerStats.get(p.getID());
                if (stat == null) {
                    playerStats.put(p.getID(), stat = createPlayerStat(p.getID()));
                    controller.addPlayerStat(stat.root);
                }
                stat.update(p, board);
                stat.active(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        playerStats.get(current).active(true);

    }

    private void startActionPerformed() {
        controller.gameStarted();
        Settings.NUMBER_OF_PLAYERS = controller.getMaxPlayer();
        if (game == null) {
            setGame(new Game());
        }
        game.setUserinterface(this);
        controller.clearPlayerStats();
        game.start();
    }

    private void stopActionPerformed() {
        if (game != null) {
            game.stopGame();
            game = null;
        }
        game = new Game();
        controller.gameStopped();
    }

    private void init3dStuff() {
        // scene graph
        scene3dRoot = new Group();

        Pane parent3d = controller.getParent3D();
        SubScene sub3d = controller.getSub3D();
        // replacing original Subscene with antialised one ...
        // TODO: do it in a nicer way!
        parent3d.getChildren().remove(sub3d);
        sub3d = new SubScene(scene3dRoot, 300, 300, true, SceneAntialiasing.BALANCED);
        parent3d.getChildren().add(0, sub3d);
        sub3d.heightProperty().bind(parent3d.heightProperty());
        sub3d.widthProperty().bind(parent3d.widthProperty());

        Translate camTranZ = new Translate(0, 0, -15);

        // Create and position camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.getTransforms().addAll(camRotateY, camRotateX, camTranZ);

        camTranZ.zProperty().bind(controller.getCamZoomSlide().valueProperty());

        // create rotation animations
        // rotate right
        AddTransition camRotR = new AddTransition(Duration.millis(3000), camRotateY.angleProperty(), 360);
        camRotR.setInterpolator(Interpolator.LINEAR);
        camRotR.setCycleCount(Animation.INDEFINITE);
        camRotR.setAutoReverse(false);
        controller.addCamRotateRightStartListener(camRotR::play);
        controller.addCamRotateRightStopListener(camRotR::stop);

        // rotate left
        AddTransition camRotL = new AddTransition(Duration.millis(3000), camRotateY.angleProperty(), -360);
        camRotL.setInterpolator(Interpolator.LINEAR);
        camRotL.setCycleCount(Animation.INDEFINITE);
        camRotL.setAutoReverse(false);
        controller.addCamRotateLeftStartListener(camRotL::play);
        controller.addCamRotateLeftStopListener(camRotL::stop);

        // rotate up
        AddTransition camRotU = new AddTransition(Duration.millis(3000), camRotateX.angleProperty(), -360);
        camRotU.setLowerLimit(-90);
        camRotU.setInterpolator(Interpolator.LINEAR);
        camRotU.setCycleCount(Animation.INDEFINITE);
        camRotU.setAutoReverse(false);
        controller.addCamRotateUpStartListener(camRotU::play);
        controller.addCamRotateUpStopListener(camRotU::stop);

        // rotate down
        AddTransition camRotD = new AddTransition(Duration.millis(3000), camRotateX.angleProperty(), 360);
        camRotD.setUpperLimit(90);
        camRotD.setInterpolator(Interpolator.LINEAR);
        camRotD.setCycleCount(Animation.INDEFINITE);
        camRotD.setAutoReverse(false);
        controller.addCamRotateDownStartListener(camRotD::play);
        controller.addCamRotateDownStopListener(camRotD::stop);

        // stop all animations when focus is lost
        primaryStage.focusedProperty().addListener((ov, o, n) -> {
            if (!n) {
                camRotR.stop();
                camRotL.stop();
                camRotU.stop();
                camRotD.stop();
            }
        });

        // add stuff to scene graph
        scene3dRoot.getChildren().add(camera);
        sub3d.setFill(Color.WHITESMOKE);
        sub3d.setCamera(camera);

        Box c1 = new Box(1, 0.1, 1);
        c1.setMaterial(new PhongMaterial(Color.RED));
        c1.setDrawMode(DrawMode.FILL);
        // scene3dRoot.getChildren().add(c1);
    }

    private List<CardFX> updateAndGetShiftedCards(PositionType shiftPos) {
        List<CardFX> cards = new LinkedList<>();
        cards.add(shiftCard);
        CardFX oldShiftCard = shiftCard;

        if (shiftPos.getCol() == 0) {
            int sRow = shiftPos.getRow();
            cards.add(shiftCard = boardCards[sRow][BOARD_WIDTH - 1]);
            for (int x = BOARD_WIDTH - 1; x > 0; cards.add(boardCards[sRow][x] = boardCards[sRow][x - 1]), x--)
                ;
            boardCards[sRow][0] = oldShiftCard;
        } else if (shiftPos.getCol() == BOARD_WIDTH - 1) {
            int sRow = shiftPos.getRow();
            cards.add(shiftCard = boardCards[sRow][0]);
            for (int x = 0; x < BOARD_WIDTH - 1; cards.add(boardCards[sRow][x] = boardCards[sRow][x + 1]), x++)
                ;
            boardCards[sRow][BOARD_WIDTH - 1] = oldShiftCard;
        } else if (shiftPos.getRow() == 0) {
            int sCol = shiftPos.getCol();
            cards.add(shiftCard = boardCards[BOARD_HEIGHT - 1][sCol]);
            for (int z = BOARD_HEIGHT - 1; z > 0; cards.add(boardCards[z][sCol] = boardCards[z - 1][sCol]), z--)
                ;
            boardCards[0][sCol] = oldShiftCard;
        } else if (shiftPos.getRow() == BOARD_HEIGHT - 1) {
            int sCol = shiftPos.getCol();
            cards.add(shiftCard = boardCards[0][sCol]);
            for (int z = 0; z < BOARD_HEIGHT - 1; cards.add(boardCards[z][sCol] = boardCards[z + 1][sCol]), z++)
                ;
            boardCards[BOARD_HEIGHT - 1][sCol] = oldShiftCard;
        }

        return cards;
    }

    private void clearBoard() {
        currentPlayer = null;
        if (players != null) {
            playerStats = new HashMap<>();
            scene3dRoot.getChildren().removeAll(players.values());
            players = null;
        }
        if (shiftCard != null) {
            shiftCard.removeFrom(scene3dRoot);
            shiftCard = null;
        }
        if (boardCards != null) {
            for (CardFX[] ca : boardCards) {
                for (CardFX c : ca) {
                    c.removeFrom(scene3dRoot);
                }
            }
        }
        this.board = null;
    }

    private void initFromBoard(Board b) {
        clearBoard();
        this.board = b;
        players = new HashMap<>();
        boardCards = new CardFX[BOARD_HEIGHT][BOARD_WIDTH];
        for (int z = 0; z < BOARD_HEIGHT; z++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                CardType ct = b.getCard(z, x);
                CardFX card3d = new CardFX(ct, scene3dRoot);
                boardCards[z][x] = card3d;
                getCardTranslateForPosition(x, z).applyTo(card3d);
                scene3dRoot.getChildren().add(card3d);
                CardType.Pin pin = ct.getPin();
                if (pin != null) {
                    pin.getPlayerID().forEach(pid -> {
                        PlayerFX player = new PlayerFX(pid, card3d);
                        players.put(pid, player);
                        scene3dRoot.getChildren().add(player);
                    });
                }
            }
        }
        CardType ct = b.getShiftCard();
        shiftCard = new CardFX(ct, scene3dRoot);
        SHIFT_CARD_TRANSLATE.applyTo(shiftCard);
        scene3dRoot.getChildren().add(shiftCard);
    }

    private void animateMove(MoveMessageType mm, Board b, long mvD, long shifD, boolean treasureReached,
                             CountDownLatch lock) {

        final Duration durBefore = Duration.millis(shifD / 3);
        final Duration durShift = Duration.millis(shifD / 3);
        final Duration durAfter = Duration.millis(shifD / 3);
        final Duration durMove = Duration.millis(mvD);

        final PlayerFX pin = currentPlayer != null ? currentPlayer : players.getOrDefault(1, null);

        PlayerStatFX playerStat = playerStats.get(currentPlayer.playerId);
        PositionType playerPosition = playerStat.getPosition();
        PositionType newPinPos = mm.getNewPinPos();

        MoveStateCalculator msc = new MoveStateCalculator(mm, b);
        List<VectorInt2> shiftedCardsPos = msc.getCardsToShift();
        VectorInt2 pushedOutCardPos = msc.getPushedOutPlayersPosition();
        VectorInt2 pushedOutNewPos = msc.getNewPlayerPosition();
        VectorInt2 shiftCardStart = msc.getShiftCardStart();
        VectorInt2 shiftDelta = msc.getShiftDelta();
        VectorInt2 preShiftPos = VectorInt2.copy(playerPosition);
        VectorInt2 postShiftPos = msc.getPlayerPositionAfterShift(preShiftPos);

        List<CardFX> shiftCards = shiftedCardsPos.stream().map(v -> boardCards[v.y][v.x]).collect(Collectors.toList());
        shiftCards.add(shiftCard);
        CardFX pushedOutCard = boardCards[pushedOutCardPos.y][pushedOutCardPos.x];
        List<PlayerFX> pushedOutPlayers = players.values().stream().filter(p -> p.getBoundCard() == pushedOutCard)
                .collect(Collectors.toList());
        Translate3D pushedOutPlayersMoveTo = getCardTranslateForPosition(pushedOutNewPos.x, pushedOutNewPos.y);
        Animation movePushedOutPlayers = AnimationFactory.moveShiftedOutPlayers(pushedOutPlayers,
                pushedOutPlayersMoveTo, shiftCard, durMove.multiply(4));

        FakeTranslateBinding pinBind = null;
        if (pin.getBoundCard() != null) {
            pinBind = new FakeTranslateBinding(pin, pin.getBoundCard(), pin.getOffset());
            pin.unbindFromCard();
            pinBind.bind();
        }
        FakeTranslateBinding pinBind_final = pinBind;

        CardFX shiftCardC = shiftCard;
        Card c = new Card(mm.getShiftCard());
        PositionType newCardPos = mm.getShiftPosition();
        int newRotation = c.getOrientation().value();
        // prevent rotating > 180°
        int oldRotation = shiftCardC.rotateProperty().intValue();
        int rotationDelta = newRotation - oldRotation;
        if (rotationDelta > 180) {
            shiftCardC.rotateProperty().setValue(oldRotation + 360);
        } else if (rotationDelta < -180) {
            shiftCardC.rotateProperty().setValue(oldRotation - 360);
        }

        Translate3D newCardBeforeShiftT = getCardTranslateForPosition(shiftCardStart.x, shiftCardStart.y); // getCardTranslateForShiftStart(newCardPos);

        // before before
        // TODO: less time for "before before" more time for "before"
        TranslateTransition animBeforeBefore = new TranslateTransition(durAfter, shiftCardC);
        // animBeforeBefore.setToX(SHIFT_CARD_TRANSLATE.x);
        animBeforeBefore.setToY(SHIFT_CARD_TRANSLATE.y);
        // animBeforeBefore.setToZ(SHIFT_CARD_TRANSLATE.z);

        // before shift
        RotateTransition cardRotateBeforeT = new RotateTransition(durBefore, shiftCardC);
        cardRotateBeforeT.setToAngle(newRotation);
        TranslateTransition cardTranslateBeforeT = new TranslateTransition(durBefore, shiftCardC);
        cardTranslateBeforeT.setToX(newCardBeforeShiftT.x);
        cardTranslateBeforeT.setToY(newCardBeforeShiftT.y);
        cardTranslateBeforeT.setToZ(newCardBeforeShiftT.z);
        Animation animBefore = new ParallelTransition(cardRotateBeforeT,
                new SequentialTransition(animBeforeBefore, cardTranslateBeforeT));

        // shifting
        // invert delta shift, because graphics coordinates are the other way
        // round!
        Translate3D shiftTranslate = new Translate3D(shiftDelta.x, 0, -shiftDelta.y);
        // getCardShiftBy(newCardPos);
        /* List<CardFX> shiftCards = */
        updateAndGetShiftedCards(newCardPos);
        Animation[] shiftAnims = new Animation[shiftCards.size()];
        int i = 0;
        for (CardFX crd : shiftCards) {
            TranslateTransition tmpT = new TranslateTransition(durShift, crd);
            tmpT.setByX(shiftTranslate.x);
            tmpT.setByY(shiftTranslate.y);
            tmpT.setByZ(shiftTranslate.z);
            shiftAnims[i++] = tmpT;
        }
        Animation animShift = new ParallelTransition(shiftAnims);

        // after
        TranslateTransition animAfter = new TranslateTransition(durAfter, shiftCard);
        animAfter.setToX(SHIFT_CARD_TRANSLATE.x);
        animAfter.setToY(SHIFT_CARD_TRANSLATE.y);
        animAfter.setToZ(SHIFT_CARD_TRANSLATE.z);

        Position from = new Position(postShiftPos.y, postShiftPos.x);
        Position to = new Position(newPinPos);
        Timeline moveAnim = AnimationFactory.createMoveTimeline(b, from, to, currentPlayer, durMove);

        // a little bit of time to switch focus from shifting to moving ^^
        Transition pause = new PauseTransition(Duration.millis(100));

        SequentialTransition allTr = new SequentialTransition(animBefore, animShift, movePushedOutPlayers, pause,
                /* animAfter, */ moveAnim);
        allTr.setInterpolator(Interpolator.LINEAR);
        allTr.setOnFinished(e -> {
            if (pinBind_final != null) {
                pinBind_final.unbind();
            }
            if (treasureReached) {
                boardCards[newPinPos.getRow()][newPinPos.getCol()].getTreasure().treasureFound();
            }
            pin.bindToCard(boardCards[newPinPos.getRow()][newPinPos.getCol()]);

            lock.countDown();
        });
        allTr.play();
    }

    @Override
    public void displayMove(MoveMessageType moveMessage, Board board, long moveDelay, long shiftDelay, boolean treasureReached) {
        CountDownLatch lock = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                animateMove(moveMessage, board, moveDelay, shiftDelay, treasureReached, lock);
            } catch (Exception e) {
                lock.countDown();
            }
        });

        // make it blocking!
        do {
            try {
                lock.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (lock.getCount() != 0);
    }

    @Override
    public void updatePlayerStatistics(List<Player> statistics, Integer currentPlayerID) {
        Platform.runLater(() -> this.updatePlayerStats(statistics, currentPlayerID));
    }

    @Override
    public void init(Board board) {
        Platform.runLater(() -> initFromBoard(board));
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
        // not needed ??? Needs further testing
        /*
		 * if (g == null) { // Platform.runLater(()->clearBoard()); } else { //
		 * g.start(); }
		 */
    }

    @Override
    public void gameEnded(Player winner) {
        Platform.runLater(() -> {
            controller.gameStopped();
            if (winner != null) {
                int playerId = winner.getID();
                PlayerStatFX stats = playerStats.get(playerId);
                stats.setWinner();
                controller.showWinner(stats.getPlayer().getName());
            }
        });

    }
}
