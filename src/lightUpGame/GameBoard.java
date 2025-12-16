package lightUpGame;

import java.util.*;

class GameBoard {
    private int[][] grid;
    private boolean[][] bulbs;
    private boolean[][] lit;
    private boolean[][] blocked;
    private int size;
    
    public GameBoard(int[][] puzzle) {
        this.size = puzzle.length;
        this.grid = puzzle;
        this.bulbs = new boolean[size][size];
        this.lit = new boolean[size][size];
        this.blocked = new boolean[size][size];
    }
    
    public int getSize() { return size; }
    
    public boolean isValidCell(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }
    
    public CellType getCellType(int row, int col) {
        if (grid[row][col] == -1) return CellType.BLACK;
        if (grid[row][col] >= 0 && grid[row][col] <= 4) return CellType.NUMBERED;
        return CellType.EMPTY;
    }
    
    public int getNumberValue(int row, int col) {
        return grid[row][col];
    }
    
    public boolean hasBulb(int row, int col) {
        return bulbs[row][col];
    }
    
    public boolean isLit(int row, int col) {
        return lit[row][col];
    }
    
    public boolean isBlocked(int row, int col) {
        return blocked[row][col];
    }
    
    public void placeBulb(int row, int col) {
        bulbs[row][col] = true;
    }
    
    public void setLit(int row, int col, boolean value) {
        lit[row][col] = value;
    }
    
    public void setBlocked(int row, int col, boolean value) {
        blocked[row][col] = value;
    }
    
    public List<Point> getAdjacentEmptyCells(int row, int col) {
        List<Point> adjacent = new ArrayList<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        
        for (int[] dir : dirs) {
            int nr = row + dir[0];
            int nc = col + dir[1];
            if (isValidCell(nr, nc) && getCellType(nr, nc) == CellType.EMPTY) {
                adjacent.add(new Point(nr, nc));
            }
        }
        return adjacent;
    }
}
