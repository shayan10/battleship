import java.util.Queue;

public class UserQueue {
	private final Queue<User> waitingUsers;
	private final Object lock = new Object();

	public UserQueue(Queue<User> queue) {
		waitingUsers = queue;
	}

	public int usersInQueue() {
		return waitingUsers.size();
	}

	public void addUser(User user) {
		synchronized (lock) {
			waitingUsers.add(user);
		}
	}
	public User removeUser() {
		synchronized (lock) {
			return waitingUsers.isEmpty() ? null : waitingUsers.remove();
		}
	}

	public void removeUser(User user) {
		waitingUsers.remove(user);
	}
}