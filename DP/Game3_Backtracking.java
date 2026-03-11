package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

// ============================================================
//  GAME 3 – Backtracking (Computer-Only, EXACT solver)
// ============================================================
//  Strategy:
//  1. Collect all empty cells into an ordered list.
//  2. Recursive backtracking: for each empty cell decide
//     PLACE_BULB or SKIP.
//  3. Prune immediately on any constraint violation.
//  4. Once the full solution is found, animate it step-by-step
//     via a Swing Timer.
// ============================================================
public class Game3_Backtracking extends JFrame {

    private static final int CELL_SIZE    = 60;
    private static final int VIS_DELAY    = 50; // Faster visualization

    // ── puzzle state ─────────────────────────────────────────
    private int     size;
    private int[][] grid;           // -1=black, 0-4=numbered, 5=empty
    
    // ── backtracking state ──────────────────────────────────
    private GameBoard   board;     // live board shown to user
    private BoardPanel3 canvas;
    private List<Point> whiteCells;
    private boolean[][] bulbs;
    private int[][]     illuminated; // Count of bulbs seeing each cell
    private Map<Point, Integer> adjCount; // Map for numbered black cells
    private Stack<BacktrackState> stack;
    private int         steps;
    private JLabel      statusLabel;
    private JLabel      stepLabel;
    private javax.swing.Timer visualizationTimer;
    private long        computeTimeNanos;

    private int[][]     sharedPuzzle;

    public Game3_Backtracking(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 3 – Backtracking");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(new Color(180, 0, 0));
        
        stepLabel = new JLabel("Steps: 0");
        stepLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JPanel bot = new JPanel(new BorderLayout());
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bot.add(statusLabel, BorderLayout.CENTER);
        bot.add(stepLabel, BorderLayout.EAST);
        root.add(bot, BorderLayout.SOUTH);
        canvas = new BoardPanel3();
        root.add(canvas, BorderLayout.CENTER);
        add(root);

        loadGame(sharedPuzzle);
        pack();
    }

    public void loadSharedGame(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        loadGame(this.sharedPuzzle);
    }

    private void loadGame(int[][] puzzle) {
        if (visualizationTimer != null) visualizationTimer.stop();
        
        size = puzzle.length;
        grid = new int[size][size];
        for (int i = 0; i < size; i++) System.arraycopy(puzzle[i], 0, grid[i], 0, size);

        board = new GameBoard(puzzle);
        canvas.setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));
        pack();
        canvas.repaint();

        whiteCells = new ArrayList<>();
        adjCount = new HashMap<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (grid[r][c] == 5) whiteCells.add(new Point(r, c));
                else if (grid[r][c] >= 0 && grid[r][c] <= 4) adjCount.put(new Point(r, c), 0);
            }
        }

        bulbs = new boolean[size][size];
        illuminated = new int[size][size];
        stack = new Stack<>();
        stack.push(new BacktrackState(0, 0)); // Start at index 0, choice 0 (Try Place)
        steps = 0;
        stepLabel.setText("Steps: 0");
        statusLabel.setText("Backtracking...");

        visualizationTimer = new javax.swing.Timer(VIS_DELAY, e -> nextBacktrackStep());
        visualizationTimer.start();
    }

    private void nextBacktrackStep() {
        long t0 = System.nanoTime();
        if (stack.isEmpty()) {
            visualizationTimer.stop();
            statusLabel.setText("No solution found! ❌");
            computeTimeNanos += (System.nanoTime() - t0);
            return;
        }

        BacktrackState current = stack.peek();
        int index = current.index;
        
        if (index == whiteCells.size()) {
            if (isCompleteSolution()) {
                visualizationTimer.stop();
                statusLabel.setText("Puzzle Solved! ✓");
                System.out.println("[G3] Solved in " + steps + " steps.");
            } else {
                stack.pop();
            }
            return;
        }

        Point c = whiteCells.get(index);
        steps++;
        stepLabel.setText("Steps: " + steps);

        if (current.choice == 0) {
            // --- BRANCH 1: PLACE BULB ---
            current.choice = 1; // Next time we come back to this state, try SKIP
            if (!createsConflict(c) && !overSatisfiesNumbered(c)) {
                current.delta = applyBulb(c);
                board.placeBulb(c.x, c.y);
                updateDisplayIllumination();
                canvas.repaint();
                stack.push(new BacktrackState(index + 1, 0));
            } else {
                // Cannot place, choice 0 finished, next iteration will handle choice 1
            }
        } else if (current.choice == 1) {
            // --- BRANCH 2: LEAVE EMPTY ---
            current.choice = 2; // Both branches tried
            
            // If we placed a bulb in choice 0, undo it
            if (current.delta != null) {
                undoBulb(c, current.delta);
                current.delta = null;
                board.removeBulb(c.x, c.y);
                updateDisplayIllumination();
                canvas.repaint();
            }

            if (canBeIlluminatedLater(c, index) && !underSatisfiesNumbered(c, index)) {
                stack.push(new BacktrackState(index + 1, 0));
            } else {
                // Pruned
            }
        } else {
            // Choice 2: Exhausted both branches
            stack.pop();
        }
    }

    private static class BacktrackState {
        int index;
        int choice; // 0=try place, 1=try skip, 2=exhausted
        BulbDelta delta; // state for undoing choice 0
        BacktrackState(int i, int c) { this.index = i; this.choice = c; }
    }

    private static class BulbDelta {
        List<Point> newlyLit;
        List<Point> adjNumbered;
        BulbDelta(List<Point> newlyLit, List<Point> adjNumbered) {
            this.newlyLit = newlyLit;
            this.adjNumbered = adjNumbered;
        }
    }

    // --- Provided Logic Functions ---

    private boolean createsConflict(Point c) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            while (inBounds(nr, nc)) {
                if (grid[nr][nc] != 5) break; 
                if (bulbs[nr][nc]) return true;
                nr += d[0]; nc += d[1];
            }
        }
        return false;
    }

    private boolean overSatisfiesNumbered(Point c) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            if (inBounds(nr, nc) && grid[nr][nc] >= 0 && grid[nr][nc] <= 4) {
                Point b = new Point(nr, nc);
                if (adjCount.get(b) + 1 > grid[nr][nc]) return true;
            }
        }
        return false;
    }

    private boolean canBeIlluminatedLater(Point c, int index) {
        if (illuminated[c.x][c.y] > 0) return true;
        // Check if any unprocessed cell in row/col can illuminate it
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            while (inBounds(nr, nc)) {
                if (grid[nr][nc] != 5) break;
                // Is this cell in the future?
                boolean processed = false;
                for (int i = 0; i <= index; i++) {
                    if (whiteCells.get(i).x == nr && whiteCells.get(i).y == nc) {
                        processed = true;
                        break;
                    }
                }
                if (!processed) return true;
                nr += d[0]; nc += d[1];
            }
        }
        return false;
    }

    private boolean underSatisfiesNumbered(Point c, int index) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            if (inBounds(nr, nc) && grid[nr][nc] >= 0 && grid[nr][nc] <= 4) {
                Point b = new Point(nr, nc);
                int current = adjCount.get(b);
                int remaining = 0;
                for (int[] d2 : dirs) {
                    int nr2 = nr + d2[0], nc2 = nc + d2[1];
                    if (inBounds(nr2, nc2) && grid[nr2][nc2] == 5) {
                        // Is this neighbor (nr2, nc2) unprocessed and not the current point c?
                        if (nr2 == c.x && nc2 == c.y) continue;
                        boolean processed = false;
                        for (int i = 0; i <= index; i++) {
                            if (whiteCells.get(i).x == nr2 && whiteCells.get(i).y == nc2) {
                                processed = true;
                                break;
                            }
                        }
                        if (!processed) remaining++;
                    }
                }
                if (current + remaining < grid[b.x][b.y]) return true;
            }
        }
        return false;
    }

    private BulbDelta applyBulb(Point c) {
        bulbs[c.x][c.y] = true;
        List<Point> newlyLit = new ArrayList<>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{0,0}}; // 0,0 is the cell itself
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            // If d is 0,0 just do once
            if (d[0] == 0 && d[1] == 0) {
                if (illuminated[c.x][c.y] == 0) newlyLit.add(new Point(c.x, c.y));
                illuminated[c.x][c.y]++;
                continue;
            }
            while (inBounds(nr, nc) && grid[nr][nc] == 5) {
                if (illuminated[nr][nc] == 0) newlyLit.add(new Point(nr, nc));
                illuminated[nr][nc]++;
                nr += d[0]; nc += d[1];
            }
        }
        List<Point> adjNumbered = new ArrayList<>();
        int[][] dirs4 = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs4) {
            int nr = c.x + d[0], nc = c.y + d[1];
            if (inBounds(nr, nc) && grid[nr][nc] >= 0 && grid[nr][nc] <= 4) {
                Point b = new Point(nr, nc);
                adjCount.put(b, adjCount.get(b) + 1);
                adjNumbered.add(b);
            }
        }
        return new BulbDelta(newlyLit, adjNumbered);
    }

    private void undoBulb(Point c, BulbDelta delta) {
        bulbs[c.x][c.y] = false;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{0,0}};
        for (int[] d : dirs) {
            int nr = c.x + d[0], nc = c.y + d[1];
            if (d[0] == 0 && d[1] == 0) {
                illuminated[c.x][c.y]--;
                continue;
            }
            while (inBounds(nr, nc) && grid[nr][nc] == 5) {
                illuminated[nr][nc]--;
                nr += d[0]; nc += d[1];
            }
        }
        for (Point b : delta.adjNumbered) {
            adjCount.put(b, adjCount.get(b) - 1);
        }
    }

    private boolean isCompleteSolution() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (grid[r][c] == 5 && illuminated[r][c] == 0) return false;
                if (grid[r][c] >= 0 && grid[r][c] <= 4) {
                    if (adjCount.get(new Point(r, c)) != grid[r][c]) return false;
                }
            }
        }
        return true;
    }

    private boolean inBounds(int r, int c) { return r >= 0 && r < size && c >= 0 && c < size; }

    private void updateDisplayIllumination() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board.getCellType(r, c) == CellType.EMPTY) {
                    board.setLit(r, c, illuminated[r][c] > 0);
                    board.setBlocked(r, c, illuminated[r][c] > 0 && !bulbs[r][c]);
                }
            }
        }
    }

    private class BoardPanel3 extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (board == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int sz = board.getSize();
            int currentCellSize = Math.min(getWidth() / sz, getHeight() / sz);
            int xOffset = (getWidth() - (sz * currentCellSize)) / 2;
            int yOffset = (getHeight() - (sz * currentCellSize)) / 2;

            for (int row = 0; row < sz; row++) {
                for (int col = 0; col < sz; col++) {
                    int x = xOffset + col * currentCellSize;
                    int y = yOffset + row * currentCellSize;
                    CellType ct = board.getCellType(row, col);
                    if (ct == CellType.BLACK) {
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                    } else if (ct == CellType.NUMBERED) {
                        g2.setColor(new Color(30, 30, 60));
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                        g2.setColor(Color.WHITE);
                        int fontSize = Math.max(10, currentCellSize / 2);
                        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (currentCellSize - fm.stringWidth(num)) / 2,
                                      y + (currentCellSize + fm.getAscent()) / 2 - 2);
                    } else {
                        g2.setColor(board.isLit(row, col) ? new Color(200, 255, 200) : new Color(245, 245, 245));
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                        if (board.hasBulb(row, col)) {
                            g2.setColor(new Color(50, 180, 50));
                            int padding = currentCellSize / 6;
                            int bulbSize = currentCellSize - padding * 2;
                            g2.fillOval(x + padding, y + padding, bulbSize, bulbSize);
                            g2.setColor(new Color(160, 255, 160));
                            int innerPadding = currentCellSize / 3;
                            int innerSize = currentCellSize - innerPadding * 2;
                            g2.fillOval(x + innerPadding, y + innerPadding, innerSize, innerSize);
                        }
                    }
                    g2.setColor(Color.GRAY);
                    g2.drawRect(x, y, currentCellSize, currentCellSize);
                }
            }
        }
    }
}
