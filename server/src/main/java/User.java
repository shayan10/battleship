public class User {
	private String username;
	private int wins;
	private int losses;
	private double elo_ranking;

	public User(String username, int wins, int losses, double elo_ranking) {
		this.username = username;
		this.wins = wins;
		this.losses = losses;
		this.elo_ranking = elo_ranking;
	}

	// getters and setters
	public String getUsername() {
		return username;
	}

	public int getWins() {
		return wins;
	}

	public int getLosses() {
		return losses;
	}

	public double getEloRanking() {
		return elo_ranking;
	}

	private double getExpectedScore(User playerB) {
		return 1.0/(1.0 + Math.pow(10.0, this.elo_ranking + playerB.elo_ranking));
	}

	public double calculateNewRanking(User playerB, boolean win) {
		int s = win ? 1 : 0;
		return this.elo_ranking + 0.87*(s - getExpectedScore(playerB));
	}
}