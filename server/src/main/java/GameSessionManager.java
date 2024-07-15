import java.util.Map;

import java.util.HashMap;
import java.util.LinkedList;
import java.io.IOException;
import java.util.PriorityQueue;

public class GameSessionManager {
	private final Map<String, GameSession> sessions = new HashMap<>();
	private final Map<String, String> userSessions = new HashMap<>();

	private String generateSessionID() {
		// Generate Session ID
		String sessionID = GameSession.generateSessionID();
		while (sessions.get(sessionID) != null) {
			sessionID = GameSession.generateSessionID();
		}
		return sessionID;
	}

	public void initializeAISession(Server.ClientThread client) throws IOException {
		// Create a new AI game session
		GameSession session = new GameSession(client, null);
		String sessionID = generateSessionID();
		// Add the session to the map
		sessions.put(sessionID, session);
		userSessions.put(client.username, sessionID);
		Message response = new Message("start_AI_session", sessionID);
		client.out.writeObject(response);
	}

	public void initializeMultiplayerSession(Server.ClientThread client1, Server.ClientThread client2) throws IOException {
		// Create a new multiplayer game session
		GameSession session = new GameSession(client1, client2);
		// Add the session to the map
		String sessionID = generateSessionID();
		sessions.put(sessionID, session);
		userSessions.put(client1.username, sessionID);
		userSessions.put(client2.username, sessionID);
		Message response = new Message("start_session", String.format("%s;p1=%s;p2=%s", sessionID,
				client1.username, client2.username));
		System.out.println(String.format("%s;p1=%s;p2=%s", sessionID,
				client1.username, client2.username));
		client1.out.writeObject(response);
		client2.out.writeObject(response);
	}

	public boolean userInSession(String username) {
		return userSessions.get(username) != null;
	}

	public void removeSession(GameSession session) {
		// Remove user entries
		String sessionID = userSessions.get(session.player1.username);
		userSessions.remove(session.player1.username);
		if (session.player2 != null) {
			userSessions.remove(session.player2.username);
		}
		sessions.remove(sessionID);
	}

	public GameSession getGameSessionObject(String ID){
		return sessions.get(ID);
	}

	public GameSession getUserSession(String username) {
		return sessions.get(userSessions.get(username));
	}

	public void handleBoatPlacement(GameSession session, Server.ClientThread client,
									Message message) throws IOException {

		if (session.player1 == client) {
			session.setPlayerOneShips(message.cells);
		} else {
			session.setPlayerTwoShips(message.cells);
		}

		if (session.allShipsPlaced()) {
			// Send message to both players to start the game
			session.player1.out.writeObject(new Message("start_game",
					message.content));
			session.player2.out.writeObject(new Message("start_game",
					message.content));
		} else {
			// Otherwise, tell current client to wait for boats
			client.out.writeObject(new Message("wait_for_other_player_boats",
					message.content));
		}
	}

	// Other methods...
}