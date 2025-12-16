package lightUpGame;

import java.util.*;

class ConstraintGraph {
    private GameBoard board;
    
    public ConstraintGraph(GameBoard board) {
        this.board = board;
    }
    
    public List<Point> getNumberedNeighbors(int row, int col) {
        List<Point> neighbors = new ArrayList<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        
        for (int[] dir : dirs) {
            int nr = row + dir[0];
            int nc = col + dir[1];
            if (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.NUMBERED) {
                neighbors.add(new Point(nr, nc));
            }
        }
        return neighbors;
    }
    
    public int countPlacedBulbs(int row, int col) {
        int count = 0;
        List<Point> adjacent = board.getAdjacentEmptyCells(row, col);
        for (Point p : adjacent) {
            if (board.hasBulb(p.x, p.y)) {
                count++;
            }
        }
        return count;
    }
    
    public int countFreeAdjacent(int row, int col) {
        int count = 0;
        List<Point> adjacent = board.getAdjacentEmptyCells(row, col);
        for (Point p : adjacent) {
            if (!board.isBlocked(p.x, p.y) && !board.hasBulb(p.x, p.y)) {
                count++;
            }
        }
        return count;
    }
    
    public void updateConstraints() {
        // This method can be used to maintain constraint state if needed
        // Currently handled inline in the solver
    }
}