//import com.sun.security.ntlm.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;

public class Server {
    HashMap<String, ClientThread> clientConnections = new HashMap<>();
    HashSet<String> users = new HashSet<>();
    ArrayList<ClientThread> clients = new ArrayList<>();
    GameSessionManager gameSessionManager = new GameSessionManager();
    DatabaseManager dbManager = new DatabaseManager();
    UserQueue userQueue = new UserQueue(new PriorityQueue<>(new UserComparator()));

    TheServer server;
    ExecutorService executor;
    ExecutorService dbExecutor;

    Server() {
        server = new TheServer();
        executor = Executors.newFixedThreadPool(5);
        dbExecutor = Executors.newFixedThreadPool(5);
        server.start();
    }

    public <T> T executeTask(Callable<T> task) {
        try {
            Future<T> result = executor.submit(task);
            return result.get();
        } catch (InterruptedException e) {
            // Handle the case where the current thread was interrupted while waiting
            Thread.currentThread().interrupt(); // Preserve interrupt status
            System.err.println("Task was interrupted");
        } catch (ExecutionException e) {
            // Handle the case where the computation threw an exception
            System.err.println("Computation threw an exception: " + e.getCause());
        }
        return null;
    }

    public void executeTask(Runnable task) {
        Future<?> future = dbExecutor.submit(task);
        try {
            future.get(); // Wait for the task to complete and throw any exceptions
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void removeClientThread(ClientThread clientThread) {
        try {
            clientThread.in.close();
            clientThread.out.close();
            users.remove(clientThread.username);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        synchronized (clients) {
            clients.remove(clientThread); // Assuming ClientThread has a getUsername method
        }
        synchronized (clientConnections) {
            clientConnections.remove(clientThread.username);

        }
    }

    public void removeAllClients() {
        for (ClientThread clientThread : clientConnections.values()) {
            try {
                clientThread.in.close();
                clientThread.out.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        clients.clear();
        clientConnections.clear();
    }

    public void updateRatingAI(String username, boolean win) {
        User aiUser = new User("AI", 0, 0, 1300);
        User currentUser = dbManager.getUser(username);
        double newRanking = currentUser.calculateNewRanking(aiUser, win);
        dbManager.incrementWins(username, newRanking);
    }

    public void updateRating(String username, String opponentUsername, boolean win) {
        User currentUser = dbManager.getUser(username);
        User opponentUser = dbManager.getUser(opponentUsername);
        double newRanking = currentUser.calculateNewRanking(opponentUser, win);
        if (win) {
            dbManager.incrementWins(username, newRanking);
        } else {
            dbManager.incrementLosses(username, newRanking);
        }
    }


    public void shutdown() {
        // Close all client connections
        removeAllClients();
        users.clear();

        // Shutdown the thread pool and wait for tasks to finish
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // Close the database connection
        dbManager.shutdown();
    }

    public class TheServer extends Thread {
        public void run() {
            try(ServerSocket mySocket = new ServerSocket(5555)){
                System.out.println("Creating Users table...");
                Future<?> future = dbExecutor.submit(dbManager);

                try {
                    future.get(); // Wait for the task to complete
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                System.out.println("Server is waiting for a player!");

                while(true) {
                    Socket socket = mySocket.accept();
                    System.out.println("A player has connected!");
                    ClientThread c = new ClientThread(socket);
                    clients.add(c);
                    c.start();
                }
            }
            catch(Exception e) {
                System.out.println("Server socket did not launch");
            }
        }
    }

    public class ClientThread extends Thread {
        Socket connection;
        ObjectInputStream in;
        ObjectOutputStream out;
        String username;

        ClientThread(Socket s){
            this.connection = s;
        }

        private void registerUser(String username) throws IOException {
            if (users.contains(username)) {
                this.out.writeObject(new Message("registration_error", null));
                return;
            }

            // Otherwise, Associate client thread with username
            synchronized (clientConnections) {
                clientConnections.put(username, this);
            }
            // Add username to
            synchronized (users) {
                users.add(username);
            }
            // Set thread object's username property
            this.username = username;
            // Add user to the database
            dbManager.addUser(username);
            // Send acknowledgement response to client
            this.out.writeObject(new Message("user_registered", null));
        }

        private void handleRequest(Message message) throws IOException {
            GameSession session = null;
            switch (message.type) {
                case "new_user":
                    String username = message.content;
                    registerUser(username);
                    break;
                case "request_session":
                    String sessionType = message.content;
                    if (sessionType.equals("AI")) {
                        gameSessionManager.initializeAISession(this);
                    } else {
                        User currentUser = dbManager.getUser(this.username);
                        userQueue.addUser(currentUser);
                        if (userQueue.usersInQueue() == 4) {
                            while (userQueue.usersInQueue() != 0) {
                                User player1 = userQueue.removeUser();
                                User player2 = userQueue.removeUser();
                                gameSessionManager.initializeMultiplayerSession(clientConnections.get(player1.getUsername()),
                                        clientConnections.get(player2.getUsername()));
                            }
                        } else {
                            this.out.writeObject(new Message("wait_for_opponent", null));
                        }
                    }
                    break;

                case "indiv_messsage":
                    Message response = new Message("indiv_message_ok", message.content, message.username,
                            message.recipient);
                    clientConnections.get(message.recipient).out.writeObject(response);
                    break;
                case "p1_boats":
                    session = gameSessionManager.getGameSessionObject(message.content);
                    // If AI Session
                    if (session.isAISession()) {
                        // Place the boats for the AI Side
                        PlaceBoatsAI placeBoatsTask = new PlaceBoatsAI(session.board2);
                        executeTask(placeBoatsTask);
                        session.setPlayerOneShips(message.cells);
                        this.out.writeObject(new Message("start_game", message.content));
                    } else {
                        gameSessionManager.handleBoatPlacement(session, this, message);
                    }
                    break;
                case "p2_boats":
                    session = gameSessionManager.getGameSessionObject(message.content);
                    gameSessionManager.handleBoatPlacement(session, this, message);
                    break;
                case "entire_boat_sunk":
                    session = gameSessionManager.getGameSessionObject(message.content);
                    if (!session.isAISession()) {
                        ClientThread otherThread = session.getOpponentThread(this);
                        otherThread.out.writeObject(new Message("entire_opponent_boat_sunk", null));
                    }
                    break;
                case "user_exit":
                    String sessionID = message.content;
                    GameSession gameSession = gameSessionManager.getUserSession(sessionID);
                    // Handle flow for gracefully exiting user before game
                    if (gameSession != null) {
                        if (!gameSession.isAISession()) {
                            gameSession.getOpponentThread(this).out.writeObject(new Message("opponent_disconnect",
                                    null));
                        }
                        gameSessionManager.removeSession(gameSession);
                    } else {
                        User currentUser = dbManager.getUser(this.username);
                        userQueue.removeUser(currentUser);
                    }
                    // Remove the client thread to clean up system resources
                    removeClientThread(this);
                case "turn":
                    session = gameSessionManager.getGameSessionObject(message.content);
                    boolean hit = false;

                    if (session.isAISession()) {
                        hit = session.playerOneTurn(message.cells.get(0));
                        if (hit) {
                            // If current player won, send win message
                            // Else, send hit message
                            if (session.playerTwoNoBoatsRemaining()) {
                                this.out.writeObject(new Message("win_game", null));
                                updateRatingAI(this.username, true);
                                gameSessionManager.removeSession(session);
                            } else {
                                this.out.writeObject(new Message("hit", null, null,
                                        message.cells));
                            }
                        } else {
                            // Send miss repsonse to the client if missed
                            this.out.writeObject(new Message("miss", null));
                        }

                        // Give a little bit of a gap before sending AI request
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Preserve interrupt status
                        }

                        PlayAI turnTask = new PlayAI(session);
                        ArrayList<ArrayList<Integer>> cellsHitByAI = executeTask(turnTask);
                        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
                        result.add(cellsHitByAI.get(1));

                        // If cells hit by AI
                        if (cellsHitByAI.get(0).get(0) == 1) {
                            // Detect if AI won
                            if (session.playerOneNoBoatsRemaining()) {
                                // If AI won, then send lose message to the player
                                this.out.writeObject(new Message("lose_game", null, null, result));
                                updateRatingAI(this.username, false);
                                gameSessionManager.removeSession(session);
                            } else {
                                // Send sink response to the current player
                                this.out.writeObject(new Message("sink", null, null, result));
                            }
                        } else {
                            // Otherwise, if AI misses, send continue response
                            this.out.writeObject(new Message("continue", null, null, result));
                        }
                        break;
                    }

                    boolean isPlayerOne = false;
                    // Execute player turn
                    if (session.player1 == this) {
                        hit = session.playerOneTurn(message.cells.get(0));
                        isPlayerOne = true;
                    } else {
                        hit = session.playerTwoTurn(message.cells.get(0));
                    }

                    ClientThread opponentThread = session.getOpponentThread(this);

                    // If cell was hit, determine game status
                    // Else, send miss message to client and continue message to opponent
                    if (hit) {
                        // If no winner yet, send hit/sink response
                        // Else, send win/lose message
                        boolean winCheck = false;
                        if (isPlayerOne) {
                            winCheck = session.playerTwoNoBoatsRemaining();
                        } else {
                            winCheck = session.playerOneNoBoatsRemaining();
                        }

                        if (!winCheck) {
                            this.out.writeObject(new Message("hit", null, null, message.cells));
                            opponentThread.out.writeObject(new Message("sink", null, null,
                                    message.cells));
                        } else {
                            this.out.writeObject(new Message("win_game", null));
                            opponentThread.out.writeObject(new Message("lose_game", null, null, message.cells));
                            updateRating(this.username, opponentThread.username, true);
                            updateRating(opponentThread.username, this.username, false);
                            gameSessionManager.removeSession(session);
                        }
                    } else {
                        this.out.writeObject(new Message("miss", null));
                        opponentThread.out.writeObject(new Message("continue", null, null,
                                message.cells));
                    }
                    break;

            }
        }

        public void run() {
            try {
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                connection.setTcpNoDelay(true);
            }
            catch (Exception e) {
                System.out.println("Streams not open");
            }

            while(true){
                try{
                    Message message = (Message)in.readObject();
                    handleRequest(message);
                }
                catch(Exception e){
                    // Remove the user from the waiting queue
                    User currentUser = dbManager.getUser(this.username);
                    userQueue.removeUser(currentUser);
                    // If the user is in a multiplayer session, send a win message to the opponent
                    GameSession session = gameSessionManager.getUserSession(this.username);

                    if (session != null) {
                        try {
                            if (!session.isAISession()) {
                                session.getOpponentThread(this).out.writeObject(new Message("opponent_disconnect",
                                        null));
                            }
                            gameSessionManager.removeSession(session);
                        } catch (IOException ioExp) {
                            // Do nothing
                        }
                    }

                    // Remove the connection
                    removeClientThread(this);
                }
            }
        }
    }
}