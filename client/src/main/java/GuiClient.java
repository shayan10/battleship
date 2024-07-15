import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;


public class GuiClient extends Application{
    private Text welcome, choose, nameError, prompt, remaining, selected, requiredBlocks, orientationSelected, error, currTurn, remainingPlayer, remainingOpponent, chatText = new Text("CHAT");
    private Button onlineButton, botButton, verticalButton, horizontalButton, confirmButton, hitButton, retryButton, quitButton;
    private Button battleship, cruiser, submarine, carrier, destroyer, selectedShip = null;
    private GridPane playerBoatPane, enemyBoatPane;
    private TextField nameTextField, messageField = new TextField();
    private HBox buttonBox, gameButtonBox, middleHBox = new HBox(50);
    ListView<String> chatLog = new ListView<>();
    private VBox welcomeBox, boatSelectBox, orientationBox, mainVBox, topTextBox, chatBox = new VBox(10, chatText, chatLog, messageField);
    private HashMap<String, Scene> sceneMap;
    private Client clientConnection;
    private final int numColumns = 10, numRows = 10, cellSize = 30;
    private String currentOrientation = null, username, opponent = null, sessionID = null, firstPlayer, secondPlayer;
    private int remainingBoats = 5, remainingOpponentBoats = 5;
    private Rectangle currChosenCell = null;
    private ArrayList<Rectangle> abc = new ArrayList<>();
    private PauseTransition playerEndTurnPause, opponentEndTurnPause;
    private ArrayList<ArrayList<Integer>> boatCells;
    private HashMap<String, ArrayList<ArrayList<Integer>>> boatCoordinates = new HashMap<>();
    private ProgressIndicator loadingIndicator = new ProgressIndicator();
    private String[] boatImages = {"shiphead.png", "shipmiddle.png", "shiptail.png", "shipheadvertical.png", "shipmiddlevertical.png", "shiptailvertical.png"};

    String welcomeSong = "src/main/resources/homepagemusic.mp3";
    Media welcomeSound = new Media(new File(welcomeSong).toURI().toString());
    MediaPlayer welcomeSoundPlayer = new MediaPlayer(welcomeSound);

    String gameplayMusic = "src/main/resources/BlackPearl.mp3";
    Media gamePlaySound = new Media(new File(gameplayMusic).toURI().toString());
    MediaPlayer gamePlaySoundPlayer = new MediaPlayer(gamePlaySound);
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        clientConnection = new Client(data->
                Platform.runLater(()->{
                    if (data instanceof Message) {
                        Message message = (Message) data;

                        switch (message.type) {
                            case "user_registered":  // New user has joined the server
                                choose.setText("Play with another player or with the AI");
                                welcomeBox.getChildren().remove(nameTextField);
                                welcomeBox.getChildren().add(buttonBox);
                                nameError.setText("");
                                break;
                            case "registration_error":  // username already exists
                                nameError.setText("Username already exists! Choose a different username");
                                break;
                            case "wait_for_opponent":
                                choose.setText("Waiting for opponent...");
                                welcomeBox.getChildren().add(loadingIndicator);
                                break;
                            case "start_session":
                                sessionID = message.content;
                                firstPlayer = sessionID.substring(sessionID.indexOf("p1=") + "p1=".length(), sessionID.indexOf("p2=") - 1);
                                secondPlayer = sessionID.substring(sessionID.indexOf("p2=") + "p2=".length());
                                sessionID = message.content.split(";")[0];

                                if (firstPlayer.equals(username)) {
                                    opponent = secondPlayer;
                                } else {
                                    opponent = firstPlayer;
                                }

                                choose.setText("Opponent Found!");

                                Text t1 = new Text("Your opponent is " + opponent);
                                t1.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-fill: white; -fx-font-family: Arial; ");
                                welcomeBox.getChildren().add(t1);
                                welcomeBox.getChildren().remove(loadingIndicator);

                                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                                pause.setOnFinished(event -> {
                                    boatPlace(primaryStage);
                                    choose.setText("Play with another player or with the AI");
                                    welcomeBox.getChildren().remove(nameTextField);
                                    welcomeBox.getChildren().add(buttonBox);
                                    nameError.setText("");
                                    t1.setText("");
                                });
                                pause.play();
                                break;
                            case "indiv_message_ok":
                                chatLog.getItems().add(message.username + " to you: " + message.content);
                                break;
                            case "start_AI_session":
                                sessionID = message.content.split(";")[0];
                                boatPlace(primaryStage);
                                break;
                            case "wait_for_other_player_boats":
                                error.setText("Waiting for opponent...");
                                break;
                            case "start_game":
                                gamePlay(primaryStage);
                                break;
                            case "entire_opponent_boat_sunk":
                                System.out.println("Opponent Boat Sunk!");
                                remainingOpponent.setText(opponent + "'s Remaining Boats: " + (--remainingOpponentBoats));
                                break;
                            case "hit":
                                currChosenCell.setFill(Color.ORANGE);
                                currChosenCell.setDisable(true);
                                currChosenCell = null;

                                System.out.println("It was a hit!");
                                playerEndTurnPause = new PauseTransition(Duration.seconds(0.5));
                                playerEndTurnEvent();
                                playerEndTurnPause.play(); // Start the delay
                                break;
                            case "miss":
                                currChosenCell.setFill(Color.BLACK);
                                currChosenCell.setDisable(true);
                                currChosenCell = null;

                                System.out.println("It was a miss.");
                                playerEndTurnPause = new PauseTransition(Duration.seconds(0.5));
                                playerEndTurnEvent();
                                playerEndTurnPause.play(); // Start the delay
                                break;
                            case "sink":
                                ArrayList<ArrayList<Integer>> sinkCell = message.cells;
                                Rectangle targetSinkCell = (Rectangle) getNodeFromGridPane(playerBoatPane, sinkCell.get(0).get(0), sinkCell.get(0).get(1));

                                System.out.println("The opponent hit at X: " + sinkCell.get(0).get(0));
                                System.out.println("The opponent hit at Y: " + sinkCell.get(0).get(1));

                                targetSinkCell.setFill(Color.INDIANRED);

                                Iterator<Map.Entry<String, ArrayList<ArrayList<Integer>>>> iterator = boatCoordinates.entrySet().iterator();

                                while (iterator.hasNext()) {
                                    Map.Entry<String, ArrayList<ArrayList<Integer>>> entry = iterator.next();
                                    ArrayList<ArrayList<Integer>> coordinates = entry.getValue();

                                    for (ArrayList<Integer> coordinate : coordinates) {
                                        if (coordinate.get(0).equals(sinkCell.get(0).get(0)) && coordinate.get(1).equals(sinkCell.get(0).get(1))) {
                                            coordinates.remove(coordinate); // Remove the coordinate if it matches

                                            // If no more coordinates, remove the boat
                                            if (coordinates.size() == 0) {

                                                iterator.remove();
                                                remainingPlayer.setText("Your Remaining Boats: " + (--remainingBoats));
                                                clientConnection.send(new Message("entire_boat_sunk", sessionID));
                                            }

                                            break;
                                        }
                                    }
                                }

                                opponentEndTurnPause = new PauseTransition(Duration.seconds(0.5));
                                opponentEndTurnEvent();
                                opponentEndTurnPause.play();
                                break;
                            case "continue":
                                ArrayList<ArrayList<Integer>> missCell = message.cells;
                                Rectangle targetMissCell = (Rectangle) getNodeFromGridPane(playerBoatPane, missCell.get(0).get(0), missCell.get(0).get(1));

                                System.out.println("The opponent missed at X: " + missCell.get(0).get(0));
                                System.out.println("The opponent missed at Y: " + missCell.get(0).get(1));

                                targetMissCell.setFill(Color.BLACK);

                                opponentEndTurnPause = new PauseTransition(Duration.seconds(0.5));
                                opponentEndTurnEvent();
                                opponentEndTurnPause.play();
                                break;
                            case "win_game":
                                currChosenCell.setFill(Color.ORANGE);
                                currChosenCell.setDisable(true);

                                PauseTransition pauseTransition = new PauseTransition(Duration.seconds(1));

                                pauseTransition.setOnFinished(event -> {
                                    currTurn.setText("Its Your Turn!");
                                });

                                pauseTransition.play();

                                remainingPlayer.setText("");
                                remainingOpponent.setText("");

                                retryButton = new Button("Retry");
                                styleRectangleButton(retryButton);
                                retryButton.setOnAction(e -> {
                                    nextGamePrep();
                                    primaryStage.setScene(sceneMap.get("welcome"));
                                });

                                quitButton = new Button("Quit");
                                styleRectangleButton(quitButton);
                                quitButton.setOnAction(e -> {
                                    Platform.exit();
                                    System.exit(0);
                                });

                                gameButtonBox.getChildren().clear();
                                gameButtonBox.getChildren().addAll(retryButton, quitButton);

                                break;
                            case "lose_game":
                                PauseTransition transition = new PauseTransition(Duration.seconds(1));
                                ArrayList<ArrayList<Integer>> loseCell = message.cells;
                                Rectangle targetLoseCell = (Rectangle) getNodeFromGridPane(playerBoatPane, loseCell.get(0).get(0), loseCell.get(0).get(1));

                                System.out.println("The opponent hit at X: " + loseCell.get(0).get(0));
                                System.out.println("The opponent hit at Y: " + loseCell.get(0).get(1));

                                targetLoseCell.setFill(Color.INDIANRED);

                                transition.setOnFinished(event -> {
                                    currTurn.setText("You Lose. Try Again?");
                                });

                                transition.play();
                                remainingPlayer.setText("");
                                remainingOpponent.setText("");

                                retryButton = new Button("Retry");
                                styleRectangleButton(retryButton);
                                retryButton.setOnAction(e -> {
                                    nextGamePrep();
                                    primaryStage.setScene(sceneMap.get("welcome"));
//                                    middleHBox.getChildren().clear();
//                                    mainVBox.getChildren().clear();
//                                    orientationBox.getChildren().clear();
//                                    chatBox.getChildren().clear();
                                });

                                quitButton = new Button("Quit");
                                styleRectangleButton(quitButton);
                                quitButton.setOnAction(e -> {
                                    Platform.exit();
                                    System.exit(0);
                                });

                                gameButtonBox.getChildren().clear();
                                gameButtonBox.getChildren().addAll(retryButton, quitButton);

                                break;
                            case "opponent_disconnect":
                                currTurn.setText("Opponent Has Disconnected.");
                                remainingPlayer.setText("");
                                remainingOpponent.setText("");

                                retryButton = new Button("Retry");
                                styleRectangleButton(retryButton);
                                retryButton.setOnAction(e -> {
                                    nextGamePrep();
                                    primaryStage.setScene(sceneMap.get("welcome"));
                                });

                                quitButton = new Button("Quit");
                                styleRectangleButton(quitButton);
                                quitButton.setOnAction(e -> {
                                    Platform.exit();
                                    System.exit(0);
                                });

                                gameButtonBox.getChildren().clear();
                                gameButtonBox.getChildren().addAll(retryButton, quitButton);

                                break;
                        }

                    }
                })
        );

        clientConnection.start();

        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        sceneMap = new HashMap<>();
        sceneMap.put("welcome", createWelcomePage());

        primaryStage.setScene(sceneMap.get("welcome"));
        primaryStage.setTitle("Battleships");
        primaryStage.show();
    }

    private void nextGamePrep() {
        choose.setText("Play with another player or with the AI");
        welcomeBox.getChildren().clear();
        welcomeBox.getChildren().addAll(welcome, choose, buttonBox);
        nameError.setText("");
        middleHBox.getChildren().clear();
        opponent = null;
        remainingBoats = 5;
        remainingOpponentBoats = 5;
    }

    private void opponentEndTurnEvent() {
        opponentEndTurnPause.setOnFinished(event -> {
            if (opponent != null) {
                topTextBox.getChildren().remove(remainingPlayer);
                topTextBox.getChildren().add(remainingOpponent);
            }
            currTurn.setText("Its Your Turn!");
            hitButton.setDisable(false);
        });
    }

    private void playerEndTurnEvent() {
        playerEndTurnPause.setOnFinished(event -> {
            if (opponent != null) {
                currTurn.setText("It's " + opponent + "'s Turn!");
                topTextBox.getChildren().remove(remainingOpponent);
                topTextBox.getChildren().add(remainingPlayer);
            }
            else {
                currTurn.setText("It's the AI's Turn.");
            }

            hitButton.setDisable(true);
        });
    }

    private Scene createWelcomePage() {
        welcomeSoundPlayer.play();
        welcome = new Text("Welcome to Battleships!");
        welcome.setStyle("-fx-font-size: 45; -fx-font-weight: bold; -fx-fill: white; -fx-font-family: Arial;");

        choose = new Text("Please Enter Your Username");
        choose.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-fill: white; -fx-font-family: Arial; ");

        nameError = new Text();
        nameError.setStyle(" -fx-font-size: 16; -fx-font-weight: bold; -fx-fill: #8f1212; -fx-font-family: Arial;");

        onlineButton = new Button("Multiplayer");
        styleButton(onlineButton, "linear-gradient(#448aff, #005ecb)", "linear-gradient(#82b1ff, #447eff)");

        botButton = new Button("AI");
        styleButton(botButton, "linear-gradient(#f0bf2b, #d4a004)", "linear-gradient(#f7e35c, #edd428)");

        nameTextField = new TextField();
        nameTextField.setMaxWidth(300);
        nameTextField.setStyle("-fx-text-fill: black; -fx-font-size: 16; -fx-background-radius: 10; -fx-font-family: Arial; -fx-pref-width: 30px;");

        onlineButton.setOnAction(e -> {
            clientConnection.send(new Message("request_session","multiplayer",nameTextField.getText()));
            welcomeBox.getChildren().remove(buttonBox);
        });

        botButton.setOnAction(e -> clientConnection.send(new Message("request_session","AI",nameTextField.getText())));

        loadingIndicator.setProgress(-1);
        loadingIndicator.setMaxSize(50, 50);
        loadingIndicator.setStyle("-fx-progress-color: #ffffff;");

        nameTextField.setOnAction(e->{
            if (nameTextField.getText().isEmpty()) {
                nameError.setText("Invalid Empty Username");
            }
            else {
                clientConnection.send(new Message("new_user",nameTextField.getText()));
                username = nameTextField.getText();
                nameError.setText("");
                nameTextField.clear();
            }
        });

        buttonBox = new HBox(40, onlineButton, botButton);
        buttonBox.setAlignment(Pos.CENTER);

        welcomeBox = new VBox(20, welcome, choose, nameError, nameTextField);
        welcomeBox.setAlignment(Pos.CENTER);

        Image backgroundImage = new Image("bg1.png", 900, 700, false, true);
        BackgroundImage bgImage = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
        welcomeBox.setBackground(new Background(bgImage));

        BorderPane pane = new BorderPane(welcomeBox);
        pane.setStyle("-fx-background-color: #383838;");

        return new Scene(pane, 900, 700);
    }

    private Scene createBoatPlaceScene(){
        boatSelectBox = new VBox(20, remaining, carrier, battleship, cruiser, submarine, destroyer);
        boatSelectBox.setAlignment(Pos.CENTER);

        orientationBox = new VBox(20, selected, requiredBlocks, orientationSelected, verticalButton, horizontalButton);
        orientationBox.setAlignment(Pos.CENTER);

        messageField.setStyle("-fx-text-fill: black; -fx-font-size: 16; -fx-font-family: Arial; -fx-pref-width: 30px; -fx-border-color: #000000; -fx-border-radius: 5");
        messageField.setBackground(Background.EMPTY);
        messageField.setOnAction(e->{
            String content = messageField.getText();
            clientConnection.send(new Message("indiv_messsage", content, username, opponent));
            chatLog.getItems().add("You to " + opponent + ": " + content);
            messageField.setText("");
        });

        chatLog.setStyle("-fx-border-color: #000000; -fx-border-width: 1; -fx-max-height: 300; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); ");
        chatLog.setBackground(Background.EMPTY);
        chatLog.setCellFactory(lv -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #333333; " + "-fx-font-weight: bold; -fx-font-size: 12px; "  + "-fx-border-color: #000000; " + "-fx-border-width: 0 0 1 0; -fx-border-radius: 5");
                }
                setBackground(Background.EMPTY);
            }
        });

        chatText.setStyle("-fx-fill: #161085; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: Arial");
        chatBox.setAlignment(Pos.CENTER);

        middleHBox.getChildren().add(playerBoatPane);
        middleHBox.getChildren().add(boatSelectBox);
        if (opponent != null) {
            middleHBox.getChildren().add(chatBox);
        }

        middleHBox.setAlignment(Pos.CENTER);

        mainVBox = new VBox(75, prompt, middleHBox, error);
        mainVBox.setAlignment(Pos.CENTER);

        Image backgroundImage = new Image("bg3.jpeg", 900, 700, false, true);
        BackgroundImage bgImage = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
        mainVBox.setBackground(new Background(bgImage));

        BorderPane pane = new BorderPane(mainVBox);
        pane.setStyle("-fx-background-color: Grey");

        BorderPane.setAlignment(prompt, Pos.CENTER);
        return new Scene(pane, 900, 700);
    }

    private void selectShip(Button ship) {
        error.setText("");
        if (selectedShip != null) {
            selectedShip.setDisable(false);
        }
        selectedShip = ship;
        ship.setDisable(true);

        selected.setText("Selected: " + ship.getText().split(" - ")[0]);
        requiredBlocks.setText("Required Blocks: " + ship.getUserData());

        middleHBox.getChildren().remove(1);
        middleHBox.getChildren().add(1, orientationBox);
    }

    private void boatPlace(Stage primaryStage) {
        prompt = new Text("Place Your Boats, " + username);
        prompt.setStyle("-fx-font-size: 36; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: Arial;");

        carrier = new Button("CARRIER - 5");
        battleship = new Button("BATTLESHIP - 4");
        cruiser = new Button("CRUISER - 3");
        submarine = new Button("SUBMARINE - 3");
        destroyer = new Button("DESTROYER - 2");

        // Length, Count of ship placement
        carrier.setUserData(5);battleship.setUserData(4);cruiser.setUserData(3);submarine.setUserData(3);destroyer.setUserData(2);
        carrier.setOnAction(e -> selectShip(carrier));battleship.setOnAction(e -> selectShip(battleship));cruiser.setOnAction(e -> selectShip(cruiser));submarine.setOnAction(e -> selectShip(submarine));destroyer.setOnAction(e -> selectShip(destroyer));
        styleRectangleButton(carrier);styleRectangleButton(battleship);styleRectangleButton(cruiser);styleRectangleButton(submarine);styleRectangleButton(destroyer);

        selected = new Text();
        selected.setWrappingWidth(100);
        selected.setTextAlignment(TextAlignment.CENTER);
        requiredBlocks = new Text();
        requiredBlocks.setWrappingWidth(100);
        requiredBlocks.setTextAlignment(TextAlignment.CENTER);
        orientationSelected = new Text("Select Orientation");
        orientationSelected.setWrappingWidth(100);
        orientationSelected.setTextAlignment(TextAlignment.CENTER);
        error = new Text("");

        selected.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");
        requiredBlocks.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");
        orientationSelected.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");
        error.setStyle(" -fx-font-size: 16; -fx-font-weight: bold; -fx-fill: #8f1212; -fx-font-family: Arial;");

        verticalButton = new Button("Vertical");
        horizontalButton = new Button("Horizontal");
        confirmButton = new Button("Confirm");
        styleRectangleButton(verticalButton);
        styleRectangleButton(horizontalButton);
        styleButton(confirmButton, "linear-gradient(#78c800, #558b2f)", "linear-gradient(#9eff56, #76d25b)");
        confirmButton.setOnAction(e -> {
            if (opponent == null || firstPlayer.equals(username)) {
                clientConnection.send(new Message("p1_boats", sessionID, username, boatCells));
            }
            else {
                clientConnection.send(new Message("p2_boats", sessionID, username, boatCells));
            }
            confirmButton.setDisable(true);
        });


        verticalButton.setOnAction(e -> {
            currentOrientation = "Vertical";
            verticalButton.setDisable(true);
            horizontalButton.setDisable(false);
            orientationSelected.setText("Orientation Selected: Vertical");
            error.setText("");
        });
        horizontalButton.setOnAction(e ->{
            currentOrientation = "Horizontal";
            horizontalButton.setDisable(true);
            verticalButton.setDisable(false);
            orientationSelected.setText("Orientation Selected: Horizontal");
            error.setText("");
        });

        remaining = new Text("Remaining Boats: " + remainingBoats);
        remaining.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");

        playerBoatPane = new GridPane();
        playerBoatPane.setAlignment(Pos.CENTER);
        boatCells = new ArrayList<>();

        initializedGridPane(playerBoatPane);

        sceneMap.put("prep", createBoatPlaceScene());
        primaryStage.setScene(sceneMap.get("prep"));
    }

    private void gamePlay(Stage primaryStage) {
        currTurn = new Text("It's Your Turn!");
        currTurn.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");

        remainingBoats = 5;
        remainingPlayer = new Text("Your Remaining Boats: " + remainingBoats);
        remainingPlayer.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");

        remainingOpponent = new Text(opponent + "'s Remaining Boats: " + remainingOpponentBoats);
        remainingOpponent.setStyle("-fx-font-size: 16; -fx-font-weight: normal; -fx-fill: black; -fx-font-family: Arial; ");

        hitButton = new Button("Hit!");
        hitButton.setAlignment(Pos.CENTER);
        styleButton(hitButton, "linear-gradient(#78c800, #558b2f)", "linear-gradient(#9eff56, #76d25b)");
        hitButton.setOnAction(e -> {
            if (currChosenCell == null) {
                return;
            }
            int col = GridPane.getColumnIndex(currChosenCell);
            int row = GridPane.getRowIndex(currChosenCell);

            ArrayList<ArrayList<Integer>> chosenCell = new ArrayList<>();
            ArrayList<Integer> tempChosenCell = new ArrayList<>();
            tempChosenCell.add(col);
            tempChosenCell.add(row);
            chosenCell.add(tempChosenCell);

            System.out.println("You hit X: " + col);
            System.out.println("You hit Y: " + row);

            clientConnection.send(new Message("turn", sessionID, username, chosenCell));
        });

        enemyBoatPane = new GridPane();
        enemyBoatPane.setAlignment(Pos.CENTER);

        initializedGridPane(enemyBoatPane);

        sceneMap.put("game", createGamePlayScene());
        primaryStage.setScene(sceneMap.get("game"));
    }

    private void initializedGridPane(GridPane pane) {
        // Add row labels (1 to 10)
        for (int col = 0; col < numColumns; col++) {
            Label colLabel = new Label(Integer.toString(col + 1));
            colLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: Arial;");
            colLabel.setMinSize(cellSize, cellSize);
            colLabel.setAlignment(Pos.CENTER);
            pane.add(colLabel, col + 1, 0); // Offset by one for the column labels
        }

        // Add column labels (A to J)
        char rowChar = 'A';
        for (int row = 0; row < numRows; row++) {
            Label rowLabel = new Label(String.valueOf((char)(rowChar + row)));
            rowLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: black; -fx-font-family: Arial;");
            rowLabel.setMinSize(cellSize, cellSize);
            rowLabel.setAlignment(Pos.CENTER);
            pane.add(rowLabel, 0, row + 1); // Offset by one for the row labels
        }

        // Populate the grid
        for (int row = 1; row <= numRows; row++) {
            for (int col = 1; col <= numColumns; col++) {
                Rectangle cell = new Rectangle(cellSize, cellSize);
                cell.setStroke(Color.BLACK);
                cell.setFill(Color.TRANSPARENT);
                cell.setUserData(true);
                int finalRow = row;
                int finalCol = col;
                if (pane == enemyBoatPane) {
                    cell.setOnMouseClicked(event -> guessCell(finalRow, finalCol));
                    cell.setOnMouseEntered(mouseEvent -> {
                        if(!cell.getFill().equals(Color.DARKGRAY)) {
                            cell.setStroke(Color.BLACK);
                            cell.setFill(Color.YELLOW);
                        }
                    });
                    cell.setOnMouseExited(mouseEvent -> {
                        if(!cell.getFill().equals(Color.DARKGRAY) ) {
                            cell.setStroke(Color.BLACK);
                            cell.setFill(Color.TRANSPARENT);
                        }
                    });
                }
                else if (pane == playerBoatPane) {
                    cell.setOnMouseClicked(event -> placeShip(finalRow, finalCol));
                    cell.setOnMouseEntered(mouseEvent -> {
                        if (selectedShip != null && currentOrientation != null) {
                            int shipSize = (int) selectedShip.getUserData();
                            if (currentOrientation.equals("Horizontal")) {
                                if (finalCol + shipSize <= numColumns+1) {
                                    boolean x = true;
                                    for (int i = 0; i < shipSize; i++) {
                                        Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol + i, finalRow);
                                        if (!Boolean.TRUE.equals(targetCell.getUserData())) {
                                            targetCell.setFill(Color.RED);
                                            x = false;
//                                            break; // Stop preview if overlap found
                                        }
                                    }
                                    if(x) {
                                        for (int i = 0; i < shipSize; i++) {
                                            Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol + i, finalRow);
                                            targetCell.setFill(Color.LIGHTGREEN);

                                            if (i == 0) {
                                                addImageToGridPane(boatImages[0], finalCol+i, finalRow);
                                            }
                                            else if (i == shipSize - 1) {
                                                addImageToGridPane(boatImages[2], finalCol+i, finalRow);
                                            }
                                            else {
                                                addImageToGridPane(boatImages[1], finalCol+i, finalRow);
                                            }
                                        }
                                    }

                                }
                            } else if (currentOrientation.equals("Vertical")) {
                                if (finalRow + shipSize <= numRows+1) {
                                    boolean x = true;
                                    for (int i = 0; i < shipSize; i++) {
                                        Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol, finalRow + i);
                                        if (!Boolean.TRUE.equals(targetCell.getUserData())) {
                                            targetCell.setFill(Color.RED);
                                            x = false;
//                                            break; // Stop preview if overlap found
                                        }

                                    }
                                    if(x) {
                                        for (int i = 0; i < shipSize; i++) {
                                            Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol, finalRow+i);
                                            if (i == 0) {
                                                addImageToGridPane(boatImages[3], finalCol, finalRow+i);
                                            }
                                            else if (i == shipSize - 1) {
                                                addImageToGridPane(boatImages[5], finalCol, finalRow+i);
                                            }
                                            else {
                                                addImageToGridPane(boatImages[4], finalCol, finalRow+i);
                                            }
                                            targetCell.setFill(Color.LIGHTGREEN);

                                        }
                                    }
                                }
                            }
                        }
                    });
                    cell.setOnMouseExited(mouseEvent -> {
                        if (selectedShip != null && currentOrientation != null) {
                            int shipSize = (int) selectedShip.getUserData();
                            if (currentOrientation.equals("Horizontal") && finalCol + shipSize <= numColumns+1) {
                                for (int i = 0; i < shipSize; i++) {
                                    Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol + i, finalRow);
                                    if (Boolean.TRUE.equals(targetCell.getUserData())) { // Check if cell is free
                                        targetCell.setFill(Color.TRANSPARENT);
                                        removeImageFromGridPane(playerBoatPane,finalCol+i,finalRow);
                                    }
                                    else{
                                        targetCell.setFill(Color.TRANSPARENT);
                                    }
                                }
                            } else if (currentOrientation.equals("Vertical") && finalRow + shipSize <= numRows+1) {
                                for (int i = 0; i < shipSize; i++) {
                                    Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, finalCol, finalRow + i);
                                    if (Boolean.TRUE.equals(targetCell.getUserData())) { // Check if cell is free
                                        removeImageFromGridPane(playerBoatPane,finalCol,finalRow+i);
                                        targetCell.setFill(Color.TRANSPARENT);
                                    }
                                    else{
                                        targetCell.setFill(Color.TRANSPARENT);
                                    }
                                }
                            }
                        }
                    });
                }
                pane.add(cell, col, row); // The grid content starts from (1,1) due to labels
            }
        }
    }

    private void guessCell(int finalRow, int finalCol) {
        if (currChosenCell != null) {
            currChosenCell.setFill(Color.TRANSPARENT);
        }
        currChosenCell = (Rectangle) getNodeFromGridPane(enemyBoatPane, finalCol, finalRow);
        currChosenCell.setFill(Color.DARKGRAY);
    }

    private Scene createGamePlayScene() {
        welcomeSoundPlayer.stop();
        gamePlaySoundPlayer.play();
        gamePlaySoundPlayer.setOnEndOfMedia(() -> {
            gamePlaySoundPlayer.seek(gamePlaySoundPlayer.getStartTime()); // Rewind to the beginning
            gamePlaySoundPlayer.play(); // Play again
        });
        // Top text box settings
        topTextBox = new VBox(15, currTurn);
        topTextBox.setAlignment(Pos.CENTER);
        topTextBox.setPadding(new Insets(10, 0, 50, 0));

        // Main game box settings
//        gameBox = new VBox();
//        gameBox.setAlignment(Pos.CENTER);

        // Button box settings
        gameButtonBox = new HBox(50, hitButton);
        gameButtonBox.setAlignment(Pos.CENTER);

        // If the player is playing against the AI
        if (opponent == null) {
            topTextBox.getChildren().add(remainingPlayer);
        } // If the current player gets to play first
        else if (firstPlayer.equals(username)) {
            topTextBox.getChildren().add(remainingOpponent);
        }
        else {
            currTurn.setText("It's " + opponent + "'s Turn!");
            hitButton.setDisable(true);
            topTextBox.getChildren().add(remainingPlayer);
        }
        HBox mainGameHBox = new HBox(25, playerBoatPane, enemyBoatPane);
        if (opponent != null) {
            mainGameHBox.getChildren().add(1, chatBox);
        }

        mainGameHBox.setAlignment(Pos.CENTER);
        VBox mainGameVBox = new VBox(25,topTextBox, mainGameHBox, gameButtonBox);
        mainGameVBox.setAlignment(Pos.CENTER);

        // Main border pane settings
        BorderPane pane = new BorderPane(mainGameVBox);
        pane.setStyle("-fx-background-color: grey");

        Image backgroundImage = new Image("bg3.jpeg", 900, 700, false, true);
        BackgroundImage bgImage = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
        mainGameVBox.setBackground(new Background(bgImage));

        // Set top, center, and right alignment
        BorderPane.setAlignment(gameButtonBox, Pos.CENTER);

        // Return the scene
        return new Scene(pane, 900, 700);
    }

    private void placeShip(int row, int col) {
        // No ship selected
        if (selectedShip == null) {
            if (remainingBoats != 0) {
                error.setText("Must Choose Boat");
            }
            return;
        }

        if (currentOrientation == null) {
            error.setText("Must Choose Orientation");
            return;
        }

        int shipSize = (int) selectedShip.getUserData();

        if (currentOrientation.equals("Horizontal")) {
            // Ship doesn't fit
            if (col + shipSize > numColumns + 1) {
                error.setText("Out of Bounds");
                return;
            }

            for (int i = 0; i < shipSize; i++) {
                Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, col + i, row);
                if(targetCell.getUserData().equals(false)) {
                    error.setText("Too close to another ship");
                    return;
                }
            }

            ArrayList<ArrayList<Integer>> newBoat = new ArrayList<>();

            for (int i = 0; i < shipSize; i++) {
                int newCol = col + i;
                Rectangle x = (Rectangle) getNodeFromGridPane(playerBoatPane,newCol, row);
                x.setFill(Color.TRANSPARENT);
                if (i == 0) {
                    addImageToGridPane(boatImages[0], newCol, row);
                }
                else if (i == shipSize - 1) {
                    addImageToGridPane(boatImages[2], newCol, row);
                }
                else {
                    addImageToGridPane(boatImages[1], newCol, row);
                }
                getNodeFromGridPane(playerBoatPane, newCol, row).setUserData(false);

                if(newCol + 1 < numColumns + 1)  {
                    getNodeFromGridPane(playerBoatPane, newCol + 1, row).setUserData(false);
                }

                if(row + 1 < numRows + 1) {
                    getNodeFromGridPane(playerBoatPane, newCol, row + 1).setUserData(false);
                }

                getNodeFromGridPane(playerBoatPane, newCol, row - 1).setUserData(false);
                getNodeFromGridPane(playerBoatPane, newCol - 1, row).setUserData(false);

                ArrayList<Integer> newBoatCells = new ArrayList<>();
                newBoatCells.add(newCol);
                newBoatCells.add(row);

                boatCells.add(newBoatCells);
                newBoat.add(newBoatCells);
            }
            boatCoordinates.put(selectedShip.getText(), newBoat);
            // Vertical placement
        } else if (currentOrientation.equals("Vertical")){
            // Ship doesn't fit
            if (row + shipSize > numRows + 1) {
                error.setText("Out of Bounds");
                return;
            }

            for (int i = 0; i < shipSize; i++) {
                Rectangle targetCell = (Rectangle) getNodeFromGridPane(playerBoatPane, col, row + i);
                if(targetCell.getUserData().equals(false)) {
                    error.setText("Too close to another ship");
                    return;
                }
            }

            ArrayList<ArrayList<Integer>> newBoat = new ArrayList<>();

            for (int i = 0; i < shipSize; i++) {
                int newRow = row + i;

                Rectangle x = (Rectangle) getNodeFromGridPane(playerBoatPane,col, newRow);
                x.setFill(Color.TRANSPARENT);

                if (i == 0) {
                    addImageToGridPane(boatImages[3], col, newRow);
                }
                else if (i == shipSize - 1) {
                    addImageToGridPane(boatImages[5], col, newRow);
                }
                else {
                    addImageToGridPane(boatImages[4], col, newRow);
                }
                getNodeFromGridPane(playerBoatPane, col, newRow).setUserData(false);

                if(col + 1 < numColumns + 1) {
                    getNodeFromGridPane(playerBoatPane, col + 1, newRow).setUserData(false);
                }

                if(newRow + 1 < numRows+1) {
                    getNodeFromGridPane(playerBoatPane, col, newRow + 1).setUserData(false);
                }

                getNodeFromGridPane(playerBoatPane, col - 1, newRow).setUserData(false);
                getNodeFromGridPane(playerBoatPane, col, newRow - 1).setUserData(false);

                ArrayList<Integer> newBoatCells = new ArrayList<>();
                newBoatCells.add(col);
                newBoatCells.add(newRow);

                boatCells.add(newBoatCells);
                newBoat.add(newBoatCells);
            }
            boatCoordinates.put(selectedShip.getText(), newBoat);
        }

        selectedShip.setDisable(true);
        selectedShip = null;
        currentOrientation = null;

        horizontalButton.setDisable(false);
        verticalButton.setDisable(false);

        remaining.setText("Remaining Boats: " + (--remainingBoats));
        orientationSelected.setText("Orientation Selected");
        error.setText("");

        middleHBox.getChildren().remove(1);
        middleHBox.getChildren().add(1, boatSelectBox);

        if (remainingBoats == 0) {
            boatSelectBox.getChildren().add(confirmButton);
        }
    }

    private Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null &&
                    GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }

    private void styleRectangleButton(Button button){

        button.setStyle("-fx-font-size: 14px; " + "-fx-background-color: " + "linear-gradient(#73777d, #959aa1)" + "; " + "-fx-text-fill: black; " + "-fx-pref-width: 120px; " + "-fx-pref-height: 40px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20;");
        button.setEffect(new DropShadow(10, Color.BLACK));

        // Hover effect
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 14px; " + "-fx-background-color: " + "linear-gradient(#a2a4a6, #bbbdbf)" + "; " + "-fx-text-fill: black; " + "-fx-pref-width: 125px; " + "-fx-pref-height: 45px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 14px; " + "-fx-background-color: " + "linear-gradient(#73777d, #959aa1)" + "; " + "-fx-text-fill: black; " + "-fx-pref-width: 120px; " + "-fx-pref-height: 40px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);"));
    }

    private void styleButton(Button button, String baseColor, String hoverColor) {
        button.setStyle("-fx-font-size: 15px; " + "-fx-background-color: " + baseColor + "; " + "-fx-text-fill: white; " + "-fx-pref-width: 100px; " + "-fx-pref-height: 20px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20;");
        button.setEffect(new DropShadow(10, Color.BLACK));

        // Hover effect
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 15px; " + "-fx-background-color: " + hoverColor + "; " + "-fx-text-fill: white; " + "-fx-pref-width: 110px; " + "-fx-pref-height: 20px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 15px; " + "-fx-background-color: " + baseColor + "; " + "-fx-text-fill: white; " + "-fx-pref-width: 100px; " + "-fx-pref-height: 20px; " + "-fx-border-radius: 20; " + "-fx-background-radius: 20; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);"));
    }

    private void addImageToGridPane(String imagePath, int column, int row) {
        // Create an image object
        Image image = new Image(imagePath);

        // Create an ImageView and set the image to it
        ImageView imageView = new ImageView(image);

        imageView.setMouseTransparent(true);
        imageView.setFitHeight(cellSize);
        imageView.setFitWidth(cellSize);
        imageView.setPreserveRatio(true);

        // Add the ImageView to the gridpane at the specified column and row
        playerBoatPane.add(imageView, column, row);
    }
    private void removeImageFromGridPane(GridPane gridPane, int column, int row) {
        Node nodeToRemove = null;
        for (Node node : gridPane.getChildren()) {
            // Check the node's column and row position
            if (GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null
                    && GridPane.getColumnIndex(node) == column && GridPane.getRowIndex(node) == row) {
                if (node instanceof ImageView) { // Additional check if you only want to remove ImageViews
                    nodeToRemove = node;
                    break;
                }
            }
        }

        if (nodeToRemove != null) {
            gridPane.getChildren().remove(nodeToRemove); // Remove the node from the grid
        }
    }
}
