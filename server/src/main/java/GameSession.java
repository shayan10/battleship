import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;

import java.util.ArrayList;
public class GameSession {
    Server.ClientThread player1;
    Server.ClientThread player2;
    // Initialized new Board for both Boards
    Board board1 = new Board();
    Board board2 = new Board();

    HashSet<String> guesses = new HashSet<>();

    private final Object lock = new Object();
    private int playerCount = 0;

    GameSession(Server.ClientThread player1, Server.ClientThread player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public static String generateSessionID() {
        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    private void incrementPlayerCount() {
        synchronized (lock) {
            playerCount++;
        }
    }

    public void setPlayerOneShips(ArrayList<ArrayList<Integer>> cells) {
        incrementPlayerCount();
        board1.placeShips(cells);
    }

    public void setPlayerTwoShips(ArrayList<ArrayList<Integer>> cells) {
        incrementPlayerCount();
        board2.placeShips(cells);
    }

    public boolean allShipsPlaced() {
        synchronized (lock) {
            return playerCount == 2;
        }
    }

    public boolean playerOneTurn(ArrayList<Integer> cell) {
        return board2.turn(cell.get(0), cell.get(1));
    }

    public boolean playerTwoTurn(ArrayList<Integer> cell) {
        return board1.turn(cell.get(0), cell.get(1));
    }

    public boolean isAISession() {
        return player2 == null;
    }

    public int whoWon() {
        if (board1.allCellsHit()) {
            return 2;
        } else if (board2.allCellsHit()) {
            return 1;
        } else {
            return -1;
        }
    }

    public boolean playerOneNoBoatsRemaining() {
        return board1.allCellsHit();
    }

    public boolean playerTwoNoBoatsRemaining() {
        return board2.allCellsHit();
    }

    public Server.ClientThread getOpponentThread(Server.ClientThread client) {
        return player1 == client ? player2 : player1;
    }
}