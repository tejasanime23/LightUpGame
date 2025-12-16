package lightUpGame;

import java.util.*;

class AlgorithmSolver {
    private GameBoard board;
    private VisibilityGraph visGraph;
    private ConstraintGraph consGraph;
    private Map<Integer, Set<Point>> components;

    public AlgorithmSolver(GameBoard board) {
        this.board = board;
        this.visGraph = new VisibilityGraph(board);
        this.consGraph = new ConstraintGraph(board);
        // Components are static based on board structure (walls)
        this.components = visGraph.findConnectedComponents();
    }

    public boolean isValidBulbPlacement(int row, int col) {
        // Check all numbered neighbors
        List<Point> numberedNeighbors = consGraph.getNumberedNeighbors(row, col);

        for (Point numbered : numberedNeighbors) {
            int required = board.getNumberValue(numbered.x, numbered.y);
            int placed = consGraph.countPlacedBulbs(numbered.x, numbered.y);
            
            // Would this placement exceed the requirement?
            if (placed + 1 > required) {
                return false;
            }
        }
        return true;
    }

    public void updateAfterBulbPlacement(int row, int col) {
        // "Modified BFS on Gv from u to: mark all visible nodes as lit, mark conflicting nodes as blocked"
        modifiedBFS(row, col);
        
        // Note: Constraint values are dynamic based on board.hasBulb, so no explicit update needed
        // unless we cached them. The ConstraintGraph getters read from board directly.
    }

    private void modifiedBFS(int startRow, int startCol) {
        // Mark the bulb itself
        board.setLit(startRow, startCol, true);
        board.setBlocked(startRow, startCol, true); // Bulb implies blocked? Yes, can't place another.

        // In Visibility Graph, edges are H and V lines. 
        // We traverse outwards.
        
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] dir : dirs) {
            int r = startRow + dir[0];
            int c = startCol + dir[1];
            while (board.isValidCell(r, c) && board.getCellType(r, c) != CellType.BLACK && board.getCellType(r, c) != CellType.NUMBERED) {
                // Mark visible node as lit
                board.setLit(r, c, true);
                
                // Mark conflicting nodes as blocked
                // Ref Algorithm: "mark conflicting nodes as blocked"
                // If a cell is lit, you cannot place a bulb there (standard Akari rule).
                // So Lit implies Blocked.
                board.setBlocked(r, c, true);
                
                r += dir[0];
                c += dir[1];
            }
        }
    }

    // Returns the single next move for the computer
    public Point findOptimalBulbPlacement() {
        // Step 1: Forced Constraint Propagation
        // We look for any immediate forced move.
        // "Initialize a queue Q with all numbered nodes... while Q not empty"
        // Since we only want ONE move, we find the first trigger.
        
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCellType(row, col) == CellType.NUMBERED) {
                    int required = board.getNumberValue(row, col);
                    int placed = consGraph.countPlacedBulbs(row, col);
                    int free = consGraph.countFreeAdjacent(row, col);

                    // Logic: if remaining(x) == free(x)
                    // remaining = required - placed
                    if (required - placed > 0 && (required - placed) == free) {
                        // All free adjacent nodes MUST have bulbs.
                        // Find the first one that doesn't have a bulb yet.
                        List<Point> adjacent = board.getAdjacentEmptyCells(row, col);
                        for (Point p : adjacent) {
                            if (!board.hasBulb(p.x, p.y) && !board.isBlocked(p.x, p.y)) {
                                return p; // Executing specific forced move
                            }
                        }
                    }
                }
            }
        }

        // Step 2: Sort Visibility Components
        // Score(C) = |C| - number of lit nodes in C
        List<ComponentScore> componentScores = new ArrayList<>();
        
        for (Map.Entry<Integer, Set<Point>> entry : components.entrySet()) {
            Set<Point> component = entry.getValue();
            int litCount = 0;
            for (Point p : component) {
                if (board.isLit(p.x, p.y)) {
                    litCount++;
                }
            }
            int score = component.size() - litCount;
            
            // "Sort all components by descending score... -> largest unlit region first"
            // Only consider components that have unlit nodes?
            // "Step 3: For each component C in sorted order: if C has unlit nodes..."
            if (score > 0) {
                componentScores.add(new ComponentScore(entry.getKey(), component, score));
            }
        }
        
        if (componentScores.isEmpty()) return null; // No unlit components?
        
        insertionSort(componentScores);

        // Step 3: Greedy Bulb Placement per Component
        for (ComponentScore cs : componentScores) {
            // "if C has unlit nodes" (handled by score > 0 check above)
            
            List<NodeScore> nodeScores = new ArrayList<>();
            
            for (Point u : cs.component) {
                // "choose node u ... such that u is not blocked"
                if (!board.isBlocked(u.x, u.y) && !board.hasBulb(u.x, u.y)) {
                    // Check if placement violates Gc
                    if (isValidBulbPlacement(u.x, u.y)) {
                        // score(u) = number of its outgoing edges in Gv
                        int score = visGraph.countOutgoingEdges(u.x, u.y);
                        nodeScores.add(new NodeScore(u, score));
                    }
                }
            }
            
            if (nodeScores.isEmpty()) continue; 
            
            // "Sort all nodes in descending order according to their scores using MERGE SORT"
            mergeSort(nodeScores, 0, nodeScores.size() - 1);
            
            // "choose node u ... such that ... u maximizes number of newly lit nodes"
            // We iterate through our candidates and pick the one that maximizes newly lit.
            // The constraint "Sort ... choose u" might imply picking from top, or just
            // that the algorithm structure requires sorting first. 
            // We will check ALL candidates to find the true MAX, fulfilling "maximizes".
            // The sort might be for tie-breaking or heuristic ordering.
            
            NodeScore bestNode = null;
            int maxNewlyLit = -1;
            
            for (NodeScore ns : nodeScores) {
                int newlyLit = calculateNewlyLit(ns.point);
                if (newlyLit > maxNewlyLit) {
                    maxNewlyLit = newlyLit;
                    bestNode = ns;
                }
            }
            
            if (bestNode != null) {
                return bestNode.point;
            }
        }

        return null;
    }

    private int calculateNewlyLit(Point p) {
        int count = 0;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        
        // Count p itself
        if (!board.isLit(p.x, p.y)) count++;
        
        for (int[] dir : dirs) {
            int r = p.x + dir[0];
            int c = p.y + dir[1];
            while (board.isValidCell(r, c) && board.getCellType(r, c) != CellType.BLACK && board.getCellType(r, c) != CellType.NUMBERED) {
                if (!board.isLit(r, c)) {
                    count++;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return count;
    }
    
    // Step 5: Termination - isGameComplete handled in GamePanel or here?
    public boolean isGameComplete() {
         for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCellType(row, col) == CellType.EMPTY && !board.isLit(row, col)) {
                    return false;
                }
            }
        }
        // Check constraints
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.getCellType(row, col) == CellType.NUMBERED) {
                    if (consGraph.countPlacedBulbs(row, col) != board.getNumberValue(row, col)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // --- Sorting Implementations ---

    private void insertionSort(List<ComponentScore> list) {
        for (int i = 1; i < list.size(); i++) {
            ComponentScore key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).score < key.score) { // Descending
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    private void mergeSort(List<NodeScore> list, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(list, left, mid);
            mergeSort(list, mid + 1, right);
            merge(list, left, mid, right);
        }
    }

    private void merge(List<NodeScore> list, int left, int mid, int right) {
        List<NodeScore> temp = new ArrayList<>();
        int i = left, j = mid + 1;
        
        while (i <= mid && j <= right) {
            if (list.get(i).score >= list.get(j).score) { // Descending
                temp.add(list.get(i++));
            } else {
                temp.add(list.get(j++));
            }
        }
        
        while (i <= mid) temp.add(list.get(i++));
        while (j <= right) temp.add(list.get(j++));
        
        for (int k = 0; k < temp.size(); k++) {
            list.set(left + k, temp.get(k));
        }
    }
}
