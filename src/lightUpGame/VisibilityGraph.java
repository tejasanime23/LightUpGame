package lightUpGame;

import java.util.*;

public class VisibilityGraph {
    private GameBoard board;
    
    public VisibilityGraph(GameBoard board) {
        this.board = board;
    }
    
    public Map<Integer, Set<Point>> findConnectedComponents() {
        Map<Integer, Set<Point>> components = new HashMap<>();
        boolean[][] visited = new boolean[board.getSize()][board.getSize()];
        int componentId = 0;
        
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCellType(row, col) == CellType.EMPTY && !visited[row][col]) {
                    Set<Point> component = new HashSet<>();
                    bfsComponent(row, col, visited, component);
                    components.put(componentId++, component);
                }
            }
        }
        
        return components;
    }
    
    private void bfsComponent(int startRow, int startCol, boolean[][] visited, Set<Point> component) {
        Queue<Point> queue = new LinkedList<>();
        queue.offer(new Point(startRow, startCol));
        visited[startRow][startCol] = true;
        component.add(new Point(startRow, startCol));
        
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            
            // In the Visibility Graph, edges exist between nodes in the same row/col 
            // with no obstacles. Standard BFS on the grid where we can move to 
            // adjacent EMPTY cells is sufficient to find the Connected Component
            // of the visibility graph, because if A sees B, they are connected. 
            // Transitivity of adjacency covers the whole "room".
            
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] dir : dirs) {
                int r = current.x + dir[0];
                int c = current.y + dir[1];
                
                if (board.isValidCell(r, c) && board.getCellType(r, c) == CellType.EMPTY) {
                   if (!visited[r][c]) {
                       visited[r][c] = true;
                       component.add(new Point(r, c));
                       queue.offer(new Point(r, c));
                   }
                }
            }
        }
    }
    
    public int countOutgoingEdges(int row, int col) {
        return countVisibleCells(row, col);
    }
    
    public int countVisibleCells(int row, int col) {
        int count = 0;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        
        for (int[] dir : dirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (board.isValidCell(r, c) && board.getCellType(r, c) == CellType.EMPTY) {
                count++;
                r += dir[0];
                c += dir[1];
            }
        }
        return count;
    }
}