import java.util.ArrayList;
import java.util.HashMap;

public class Board {
    HashMap<String, Integer> cells;
    private int cellsRemaining;
    Board() {
        cells = new HashMap<>();
        cellsRemaining = 0;
    }

    private String getKey(int x, int y) {
        return String.format("(%d, %d)", x, y);
    }

    public boolean isHit(int x, int y) {
        String key = getKey(x, y);
        Integer result = cells.get(key);
        return result != null && (result == 1);
    }

    public void placeShips(ArrayList<ArrayList<Integer>> shipCells) {
        // Place ships on the board
        for (ArrayList<Integer> cell: shipCells) {
            cells.put(getKey(cell.get(0), cell.get(1)), 0);
        }
        cellsRemaining = 17;
    }

    public boolean turn(int x, int y) {
        Integer entry = cells.get(getKey(x, y));
        if (entry == null || entry == 1) {
            return false;
        }
        // Remove cell from the HashMap
        cells.put(getKey(x,y),1);
        cellsRemaining--;
        return true;
    }

    public boolean allCellsHit() {
        return cellsRemaining == 0;
    }
}