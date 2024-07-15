import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Callable;

public class PlayAI implements Callable<ArrayList<ArrayList<Integer>>> {
	final GameSession session;
	Random rand = new Random();

	String g = new String();

	public PlayAI(GameSession session) {
		this.session = session;
	}

	private int generateCoordinate() {
		return rand.nextInt(9);
	}

	@Override
	public ArrayList<ArrayList<Integer>> call() {
		int x = generateCoordinate() + 1;
		int y = generateCoordinate() + 1;

		g = "" + x+y;

		while (session.guesses.contains(g)) {
			x = generateCoordinate() + 1;
			y = generateCoordinate() + 1;
			g = "" + x +y;
		}

		session.guesses.add(g);

		ArrayList<Integer> result = new ArrayList<>();
		ArrayList<Integer> guess = new ArrayList<Integer>(Arrays.asList(x, y));

		boolean hit = session.playerTwoTurn(guess);

		ArrayList<ArrayList<Integer>> resultAndGuess = new ArrayList<>();

		if (!hit) {
			result.add(0);
		}
		else {
			result.add(1);
		}
		resultAndGuess.add(result);
		resultAndGuess.add(guess);
		return resultAndGuess;
	}
}