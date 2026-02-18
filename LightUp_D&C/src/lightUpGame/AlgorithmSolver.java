package lightUpGame;

import java.util.*;

class AlgorithmSolver {
    private GameBoard board;
    private int size;

    // Phase 0: Game Initialization
    public AlgorithmSolver(GameBoard board) {
        this.board = board;
        this.size = board.getSize();
    }

    // Phase 1: User Move Validation
    public boolean isValidBulbPlacement(int row, int col) {
        // 1. Check if two bulbs see each other
        if (isVisibleToAnotherBulb(row, col)) {
            System.out.println("Invalid Move: bulb at (" + row + "," + col + ") sees another bulb.");
            return false;
        }

        // 2. Check numbered cell constraints
        // Check neighbors for any numbered cells that would be overfilled
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            if (board.isValidCell(r, c) && board.getCellType(r, c) == CellType.NUMBERED) {
                int required = board.getNumberValue(r, c);
                int placed = countPlacedBulbsAround(r, c);
                // placing one more bulb
                if (placed + 1 > required) {
                    System.out.println("Invalid Move: bulb at (" + row + "," + col + ") violates number constraint at (" + r + "," + c + ").");
                    return false;
                }
            }
        }
        return true;
    }

    public void updateAfterBulbPlacement(int row, int col) {
        // Update illumination for the new bulb
        updateIllumination();
    }
    
    // Updates the board's lit status based on all placed bulbs
    private void updateIllumination() {
        
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                
                if (board.getCellType(r, c) == CellType.EMPTY) {
                    board.setLit(r, c, board.hasBulb(r, c)); 
                    board.setBlocked(r, c, false); 
                }
            }
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board.hasBulb(r, c)) {
                    propagateLight(r, c);
                }
            }
        }
    }

    private void propagateLight(int r, int c) {
        board.setLit(r, c, true);
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            while (board.isValidCell(nr, nc)) {
                if (board.getCellType(nr, nc) == CellType.BLACK || board.getCellType(nr, nc) == CellType.NUMBERED) {
                    break;
                }
                board.setLit(nr, nc, true);
                board.setBlocked(nr, nc, true); 
                
                nr += dir[0];
                nc += dir[1];
            }
        }
    }

    private boolean isVisibleToAnotherBulb(int r, int c) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            while (board.isValidCell(nr, nc)) {
                CellType type = board.getCellType(nr, nc);
                if (type == CellType.BLACK || type == CellType.NUMBERED) break;
                if (board.hasBulb(nr, nc)) return true;
                nr += dir[0];
                nc += dir[1];
            }
        }
        return false;
    }

    private int countPlacedBulbsAround(int r, int c) {
        int count = 0;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (board.isValidCell(nr, nc) && board.hasBulb(nr, nc)) {
                count++;
            }
        }
        return count;
    }

    // Main entry point for Computer Move
    public Point findOptimalBulbPlacement() {
        // Phase 2: Deduction Engine
        Point deductedMove = runDeductionPhase(this.board); // Run on main board
        if (deductedMove != null) {
            System.out.println("Deduction Engine found forced move: " + deductedMove);
            return deductedMove;
        }
        
        System.out.println("No force moves. Starting Divide & Conquer...");
        
        // Create initial solver state
        SolverState initialState = new SolverState(this.board);
        
        // Attempt to solve
        SolverState solution = solveRecursive(initialState, 0, 0, size, size);
        
        if (solution != null) {
            System.out.println("Solution found!");
            // Find a bulb in solution that isn't in current board
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (solution.hasBulb(r, c) && !board.hasBulb(r, c)) {
                        return new Point(r, c);
                    }
                }
            }
        } else {
            System.out.println("No solution found by D&C.");
        }
        
        return null;
    }

    private Point runDeductionPhase(GameBoard targetBoard) {
        return runDeductionPhase();
    }

    private Point runDeductionPhase() {
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Iterate over all numbered cells for Rules A, B, C
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board.getCellType(r, c) == CellType.NUMBERED) {
                        int k = board.getNumberValue(r, c);
                        int placed = countPlacedBulbsAround(r, c);
                        int free = countFreeNeighbors(r, c); // Neighbors that CAN have a bulb

                        // Rule B: Overfilled Check
                        if (placed > k) {
                            System.out.println("Contradiction: Overfilled at (" + r + "," + c + ")");
                            return null; 
                        }

                        // Rule C: Zero Rule (Effective k=0 -> all neighbors forbidden)
                        if (placed == k && free > 0) {
                             if (markRemainingNeighborsForbidden(r, c)) {
                                 changed = true;
                             }
                        }

                        // Rule A: Number Satisfaction
                        int needed = k - placed;
                        if (needed > 0 && needed == free) {
                            List<Point> neighbors = getFreeNeighbors(r, c);
                            if (!neighbors.isEmpty()) {
                                return neighbors.get(0);
                            }
                        }
                    }
                }
            }

            // Rule D: Isolation Rule
             for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board.getCellType(r, c) == CellType.EMPTY && !board.isLit(r, c) && !board.hasBulb(r, c)) {
                         List<Point> potentialSources = getPotentialSources(r, c);
                         
                         if (potentialSources.isEmpty()) {
                             System.out.println("Contradiction: Cell (" + r + "," + c + ") cannot be lit.");
                             return null;
                         }
                         
                         if (potentialSources.size() == 1) {
                             Point source = potentialSources.get(0);
                             if (!board.hasBulb(source.x, source.y) && !board.isBlocked(source.x, source.y)) {
                                  return source;
                             }
                         }
                    }
                }
            }
        }
        return null;
    }

    // Helper methods for main board deduction
    private int countFreeNeighbors(int r, int c) {
        int count = 0;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (board.isValidCell(nr, nc)) {
                if (board.getCellType(nr, nc) == CellType.EMPTY && !board.hasBulb(nr, nc) && !board.isBlocked(nr, nc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<Point> getFreeNeighbors(int r, int c) {
        List<Point> res = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
             if (board.isValidCell(nr, nc)) {
                if (board.getCellType(nr, nc) == CellType.EMPTY && !board.hasBulb(nr, nc) && !board.isBlocked(nr, nc)) {
                    res.add(new Point(nr, nc));
                }
            }
        }
        return res;
    }

    private boolean markRemainingNeighborsForbidden(int r, int c) {
        boolean changed = false;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
             if (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.EMPTY 
                 && !board.hasBulb(nr, nc) && !board.isBlocked(nr, nc)) {
                board.setBlocked(nr, nc, true);
                changed = true;
            }
        }
        return changed;
    }
    
    // Finds all valid bulb locations that can light (r, c)
    private List<Point> getPotentialSources(int r, int c) {
        List<Point> sources = new ArrayList<>();
        if (canPlaceBulb(r, c)) sources.add(new Point(r, c));

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            while (board.isValidCell(nr, nc)) {
                CellType type = board.getCellType(nr, nc);
                if (type == CellType.BLACK || type == CellType.NUMBERED) break;
                
                if (canPlaceBulb(nr, nc)) {
                    sources.add(new Point(nr, nc));
                }
                 nr += dir[0];
                 nc += dir[1];
            }
        }
        return sources;
    }

    private boolean canPlaceBulb(int r, int c) {
        return board.getCellType(r, c) == CellType.EMPTY 
            && !board.hasBulb(r, c) 
            && !board.isBlocked(r, c)
            && isValidBulbPlacement(r, c); 
    }


    // Phase 3-9: Recursion
    private SolverState solveRecursive(SolverState state, int r, int c, int h, int w) {
        // Phase 9: Base Case
        if (h * w <= 4) { // Small enough (e.g. 2x2)
            return solveLocal(state, r, c, h, w);
        }

        // Phase 2 (Internal): Deduction on this state
        // If deduction fails (contradiction), return null.
        if (!state.deduce()) return null;

        // Phase 3: Choose Separator
        boolean splitHorizontal = (h >= w);
        int bestIndex = -1;
        int minWhite = Integer.MAX_VALUE;
        
        if (splitHorizontal) {
            for (int i = r + 2; i < r + h - 2; i++) {
                int whites = 0;
                for (int j = c; j < c + w; j++) {
                    if (state.getCellType(i, j) == CellType.EMPTY) whites++;
                }
                if (whites < minWhite) {
                    minWhite = whites;
                    bestIndex = i;
                }
            }
            if (bestIndex == -1) bestIndex = r + h / 2;
        } else {
             for (int j = c + 2; j < c + w - 2; j++) {
                int whites = 0;
                for (int i = r; i < r + h; i++) {
                    if (state.getCellType(i, j) == CellType.EMPTY) whites++;
                }
                if (whites < minWhite) {
                    minWhite = whites;
                    bestIndex = j;
                }
             }
             if (bestIndex == -1) bestIndex = c + w / 2;
        }

        // Phase 4: Interface State Loop
        // List of white cells in separator
        List<Point> separatorCells = new ArrayList<>();
        if (splitHorizontal) {
             for (int j = c; j < c + w; j++) {
                 if (state.getCellType(bestIndex, j) == CellType.EMPTY) {
                     separatorCells.add(new Point(bestIndex, j));
                 }
             }
        } else {
             for (int i = r; i < r + h; i++) {
                 if (state.getCellType(i, bestIndex) == CellType.EMPTY) {
                     separatorCells.add(new Point(i, bestIndex));
                 }
             }
        }

        // Iterate all 4^k states? 
        // 4 states: 0: NO_LIGHT, 1: LIGHT_FROM_1, 2: LIGHT_FROM_2, 3: BULB
        int numCells = separatorCells.size();
        if (numCells > 10) { 
             // Too many states, fallback to local or heuristic?
             // Or pick a better separator?
             return solveLocal(state, r, c, h, w); 
        }
        
        long totalStates = (long) Math.pow(4, numCells);
        for (long s = 0; s < totalStates; s++) {
            SolverState nextState = new SolverState(state);
            long tempS = s;
            boolean possible = true;
            
            for (Point p : separatorCells) {
                int type = (int)(tempS % 4);
                tempS /= 4;
                
                // Phase 5: Apply Boundary Conditions
                if (!applyInterfaceCondition(nextState, p, type, splitHorizontal, bestIndex)) {
                    possible = false;
                    break;
                }
            }
            
            if (possible) {
                // Phase 6: Conquer (Recurse)
                // Split into G1 and G2
                SolverState res1;
                if (splitHorizontal) {
                    res1 = solveRecursive(nextState, r, c, bestIndex - r, w);
                } else {
                    res1 = solveRecursive(nextState, r, c, h, bestIndex - c);
                }
                
                if (res1 != null) {
                    SolverState res2;
                    if (splitHorizontal) {
                        res2 = solveRecursive(res1, bestIndex + 1, c, r + h - (bestIndex + 1), w);
                    } else {
                        res2 = solveRecursive(res1, r, bestIndex + 1, h, c + w - (bestIndex + 1));
                    }
                    
                    if (res2 != null) {
                        return res2; // Phase 7: Valid globally (implicit by successful recursion)
                    }
                }
            }
        }

        // Phase 8: Separator Change (Implicitly we just return null if this separator failed)
        return null;
    }

    private boolean applyInterfaceCondition(SolverState state, Point p, int type, boolean splitHorizontal, int sepIdx) {
        // type 0: NO_BULB
        // type 1: LIGHT_G1_TO_G2
        // type 2: LIGHT_G2_TO_G1
        // type 3: BULB
        
        int r = p.x;
        int c = p.y;
        
        // Basic check: can we verify consistency?
        if (type == 3) {
            if (state.canPlaceBulb(r, c)) {
                state.placeBulb(r, c);
                return true;
            }
            return false;
        } else if (type == 1) { // G1 lights G2
             // Cell must be lit
             state.setLit(r, c, true);
             // Must NOT have bulb
             state.setBlocked(r, c, true); // Block placement
             // Propagate light into G2
             return state.propagateRay(r, c, splitHorizontal ? 1 : 0, splitHorizontal ? 0 : 1); 
        } else if (type == 2) { // G2 lights G1
             state.setLit(r, c, true);
             state.setBlocked(r, c, true);
             // Propagate light into G1
             return state.propagateRay(r, c, splitHorizontal ? -1 : 0, splitHorizontal ? 0 : -1);
        } else { // NO_LIGHT
             
             return true;
        }
    }

    private SolverState solveLocal(SolverState state, int r, int c, int h, int w) {
        
        List<Point> empties = state.getEmpties(r, c, h, w);
        return backtrack(state, empties, 0);
    }
    
    private SolverState backtrack(SolverState state, List<Point> empties, int idx) {
        if (idx == empties.size()) {
            return state.isValid() ? state : null;
        }
        
        Point p = empties.get(idx);
        
        // Try placing bulb
        if (state.canPlaceBulb(p.x, p.y)) {
            SolverState next = new SolverState(state);
            next.placeBulb(p.x, p.y);
            if (next.deduce()) {
                SolverState res = backtrack(next, empties, idx + 1);
                if (res != null) return res;
            }
        }
        
        
        SolverState next = new SolverState(state);
        // Maybe mark as "must be lit later"?
        SolverState res = backtrack(next, empties, idx + 1);
        if (res != null) return res;
        
        return null;
    }

    // --- SolverState Inner Class ---
    private class SolverState {
        int[][] grid; // -1 black, 0-4 numbers, 5 empty
        boolean[][] bulbs;
        boolean[][] lit;
        boolean[][] blocked;
        int size;
        
        SolverState(GameBoard board) {
            this.size = board.getSize();
            this.grid = new int[size][size];
            this.bulbs = new boolean[size][size];
            this.lit = new boolean[size][size];
            this.blocked = new boolean[size][size];
            
            for(int i=0; i<size; i++) {
                for(int j=0; j<size; j++) {
                    this.grid[i][j] = board.getNumberValue(i, j); 
                    
                    this.bulbs[i][j] = board.hasBulb(i, j);
                    this.lit[i][j] = board.isLit(i, j);
                    this.blocked[i][j] = board.isBlocked(i, j);
                }
            }
        }
        
        SolverState(SolverState other) {
            this.size = other.size;
            this.grid = new int[size][size];
            this.bulbs = new boolean[size][size];
            this.lit = new boolean[size][size];
            this.blocked = new boolean[size][size];
            for(int i=0; i<size; i++) {
                System.arraycopy(other.grid[i], 0, this.grid[i], 0, size);
                System.arraycopy(other.bulbs[i], 0, this.bulbs[i], 0, size);
                System.arraycopy(other.lit[i], 0, this.lit[i], 0, size);
                System.arraycopy(other.blocked[i], 0, this.blocked[i], 0, size);
            }
        }
        
        CellType getCellType(int r, int c) {
            if (grid[r][c] == -1) return CellType.BLACK;
            if (grid[r][c] >= 0 && grid[r][c] <= 4) return CellType.NUMBERED;
            return CellType.EMPTY;
        }

        boolean canPlaceBulb(int r, int c) {
             if (getCellType(r, c) != CellType.EMPTY) return false;
             if (bulbs[r][c]) return false; // Already has bulb
             if (blocked[r][c]) return false;
             // Check immediate visibility constraint (not seeing other bulbs)
             if (isVisibleToAnotherBulb(r, c)) return false;
             // Check number constraints around
             if (violatesNumberConstraint(r, c)) return false;
             return true;
        }
        
        void placeBulb(int r, int c) {
            bulbs[r][c] = true;
            propagateLight(r, c);
        }
        
        void setLit(int r, int c, boolean val) { lit[r][c] = val; }
        void setBlocked(int r, int c, boolean val) { blocked[r][c] = val; }
        
        boolean isLit(int r, int c) { return lit[r][c]; }
        boolean hasBulb(int r, int c) { return bulbs[r][c]; }
        
        
        
        void propagateLight(int r, int c) {
            setLit(r, c, true);
            setBlocked(r, c, true);
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                while (isValid(nr, nc)) {
                    if (getCellType(nr, nc) != CellType.EMPTY) break;
                    setLit(nr, nc, true);
                    setBlocked(nr, nc, true);
                    nr += dir[0];
                    nc += dir[1];
                }
            }
        }
        
        boolean propagateRay(int r, int c, int dr, int dc) {
            int nr = r + dr;
            int nc = c + dc;
             while (isValid(nr, nc)) {
                if (getCellType(nr, nc) != CellType.EMPTY) break;
                if (bulbs[nr][nc]) return false; // Clash with existing bulb
                setLit(nr, nc, true);
                setBlocked(nr, nc, true);
                nr += dr;
                nc += dc;
            }
            return true;
        }

        boolean isValid(int r, int c) {
            return r >= 0 && r < size && c >= 0 && c < size;
        }

        boolean deduce() {
            boolean changed = true;
            while (changed) {
                changed = false;
                
                // Rules A, B, C
                for (int r = 0; r < size; r++) {
                    for (int c = 0; c < size; c++) {
                        if (getCellType(r, c) == CellType.NUMBERED) {
                            int k = grid[r][c];
                            int placed = countPlacedBulbsAround(r, c);
                            int free = countFreeNeighbors(r, c);

                            // Rule B
                            if (placed > k) return false; // Contradiction

                            // Rule C
                            if (placed == k && free > 0) {
                                if (markRemainingNeighborsForbidden(r, c)) changed = true;
                            }

                            // Rule A
                            int needed = k - placed;
                            if (needed > 0 && needed == free) {
                                List<Point> neighbors = getFreeNeighbors(r, c);
                                for (Point p : neighbors) {
                                    if (canPlaceBulb(p.x, p.y)) {
                                        placeBulb(p.x, p.y);
                                        changed = true;
                                    } else {
                                        return false; // Contradiction: Forced move invalid
                                    }
                                }
                            }
                        }
                    }
                }

                // Rule D: Isolation
                for (int r = 0; r < size; r++) {
                    for (int c = 0; c < size; c++) {
                        if (getCellType(r, c) == CellType.EMPTY && !lit[r][c] && !bulbs[r][c]) {
                             if (blocked[r][c]) {
                                 
                             }
                             
                             List<Point> sources = getPotentialSources(r, c);
                             if (sources.isEmpty()) return false; // Cannot be lit
                             
                             if (sources.size() == 1) {
                                 Point p = sources.get(0);
                                 if (!bulbs[p.x][p.y]) {
                                     if (canPlaceBulb(p.x, p.y)) {
                                         placeBulb(p.x, p.y);
                                         changed = true;
                                     } else {
                                         return false;
                                     }
                                 }
                             }
                        }
                    }
                }
            }
            return true;
        }

        int countPlacedBulbsAround(int r, int c) {
            int count = 0;
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (isValid(nr, nc) && bulbs[nr][nc]) count++;
            }
            return count;
        }

        int countFreeNeighbors(int r, int c) {
            int count = 0;
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (isValid(nr, nc) && getCellType(nr, nc) == CellType.EMPTY 
                    && !bulbs[nr][nc] && !blocked[nr][nc]) {
                        
                        count++;
                }
            }
            return count;
        }

        List<Point> getFreeNeighbors(int r, int c) {
            List<Point> res = new ArrayList<>();
             int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (isValid(nr, nc) && getCellType(nr, nc) == CellType.EMPTY 
                    && !bulbs[nr][nc] && !blocked[nr][nc]) {
                        res.add(new Point(nr, nc));
                }
            }
            return res;
        }

        boolean markRemainingNeighborsForbidden(int r, int c) {
            boolean ch = false;
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                 if (isValid(nr, nc) && getCellType(nr, nc) == CellType.EMPTY 
                    && !bulbs[nr][nc] && !blocked[nr][nc]) {
                        setBlocked(nr, nc, true);
                        ch = true;
                }
            }
            return ch;
        }

        List<Point> getPotentialSources(int r, int c) {
            List<Point> sources = new ArrayList<>();
            if (canPlaceBulb(r, c)) sources.add(new Point(r, c));

            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                while (isValid(nr, nc)) {
                    if (getCellType(nr, nc) != CellType.EMPTY) break;
                    if (canPlaceBulb(nr, nc)) sources.add(new Point(nr, nc));
                    nr += dir[0];
                    nc += dir[1];
                }
            }
            return sources;
        }
        
        boolean isValid() {
            // Check if all white cells lit and numbers satisfied
            for(int i=0; i<size; i++) {
                for(int j=0; j<size; j++) {
                    if (getCellType(i, j) == CellType.NUMBERED) {
                        int count = 0;
                         int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                        for (int[] dir : dirs) {
                            int nr = i + dir[0];
                            int nc = j + dir[1];
                            if (isValid(nr, nc) && bulbs[nr][nc]) count++;
                        }
                        if (count != grid[i][j]) return false;
                    }
                    if (getCellType(i, j) == CellType.EMPTY && !lit[i][j]) return false;
                }
            }
            return true;
        }

        List<Point> getEmpties(int r, int c, int h, int w) {
            List<Point> res = new ArrayList<>();
             for(int i=r; i<r+h; i++) {
                for(int j=c; j<c+w; j++) {
                    if(getCellType(i, j) == CellType.EMPTY && !bulbs[i][j]) {
                        res.add(new Point(i, j));
                    }
                }
            }
            return res;
        }
        
        boolean isVisibleToAnotherBulb(int r, int c) {
             int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                while (isValid(nr, nc)) {
                    if (getCellType(nr, nc) != CellType.EMPTY) break;
                    if (bulbs[nr][nc]) return true;
                    nr += dir[0];
                    nc += dir[1];
                }
            }
            return false;
        }
        
        boolean violatesNumberConstraint(int r, int c) {
             int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (isValid(nr, nc) && getCellType(nr, nc) == CellType.NUMBERED) {
                    int req = grid[nr][nc];
                    int already = 0;
                     int[][] d2 = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    for(int[] dd : d2) {
                        int nnr = nr + dd[0];
                        int nnc = nc + dd[1];
                        if (isValid(nnr, nnc) && bulbs[nnr][nnc]) already++;
                    }
                    if (already + 1 > req) return true;
                }
            }
            return false;
        }
    }

    
    public boolean isGameComplete() {
         for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board.getCellType(row, col) == CellType.EMPTY && !board.isLit(row, col)) {
                    return false;
                }
            }
        }
        // Check constraints
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (board.getCellType(row, col) == CellType.NUMBERED) {
                    if (countPlacedBulbsAround(row, col) != board.getNumberValue(row, col)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
