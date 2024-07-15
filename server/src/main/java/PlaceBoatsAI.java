import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;

public class PlaceBoatsAI implements Callable<Void> {
	final private Board board;
	private Random random = new Random();
	public PlaceBoatsAI(Board board) {
		this.board = board;
	}

	@Override
	public Void call() {
		ArrayList<Integer> boatLengths = new ArrayList<>(Arrays.asList(2, 3, 3, 4, 5));
		Collections.shuffle(boatLengths);
		HashSet<String> occupiedCells = new HashSet<>();

		for (int boatLength : boatLengths) {
			boolean placed = false;
			while (!placed) {
				int startRow = random.nextInt(10) + 1;  // Random starting row
				int startCol = random.nextInt(10) + 1;  // Random starting column
				boolean horizontal = random.nextBoolean();  // Random orientation

				ArrayList<ArrayList<Integer>> shipCells = new ArrayList<>();
				boolean isOccupied = false;

				// Check positions and buffer zones
				if (horizontal && startCol + boatLength - 1 <= 10) {
					if (!isAreaOccupied(startRow, startCol, boatLength, horizontal, occupiedCells)) {
						for (int k = 0; k < boatLength; k++) {
							shipCells.add(new ArrayList<>(Arrays.asList(startRow, startCol + k)));
						}
					} else {
						isOccupied = true;
					}
				} else if (!horizontal && startRow + boatLength - 1 <= 10) {
					if (!isAreaOccupied(startRow, startCol, boatLength, horizontal, occupiedCells)) {
						for (int k = 0; k < boatLength; k++) {
							shipCells.add(new ArrayList<>(Arrays.asList(startRow + k, startCol)));
						}
					} else {
						isOccupied = true;
					}
				}

				// If position is valid and free, place ship
				if (!isOccupied && shipCells.size() == boatLength) {
					board.placeShips(shipCells);
					markOccupiedAreas(shipCells, occupiedCells);
					placed = true;
				}
			}
		}
		return null;
	}

	private boolean isAreaOccupied(int row, int col, int length, boolean horizontal, HashSet<String> occupiedCells) {
		for (int i = -1; i <= length; i++) {
			for (int j = -1; j <= 1; j++) {
				int checkRow = horizontal ? row + j : row + i;
				int checkCol = horizontal ? col + i : col + j;
				if (occupiedCells.contains(getCoordinateKey(checkRow, checkCol))) {
					return true;
				}
			}
		}
		return false;
	}

	private void markOccupiedAreas(ArrayList<ArrayList<Integer>> shipCells, HashSet<String> occupiedCells) {
		for (ArrayList<Integer> cell : shipCells) {
			int row = cell.get(0);
			int col = cell.get(1);
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					occupiedCells.add(getCoordinateKey(row + i, col + j));
				}
			}
		}
	}

	private String getCoordinateKey(int x, int y) {
		return String.format("(%d, %d)", x, y);
	}
}
