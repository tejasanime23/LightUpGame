package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

// ============================================================
//  GAME 3 – DP + Backtracking  (Computer-Only, EXACT solver)
// ============================================================
//  Strategy (guarantees 100% correct solution if one exists):
//
//  1. Collect all empty cells into an ordered list.
//  2. Recursive backtracking: for each empty cell decide
//     PLACE_BULB or SKIP.
//  3. Prune immediately on any constraint violation.
//  4. Memoisation (DP): encode visited partial states as a
//     bitmask-based key (Long) and cache failed sub-problems
//     in a HashSet to avoid re-exploring them.
//  5. Once the full solution is found, animate it step-by-step
//     via a Swing Timer (one bulb per 400 ms).
// ============================================================
public class Game3_DPBacktracking extends JFrame {

    private static final int CELL_SIZE   = 60;
    private static final int REVEAL_DELAY = 400;

    // ── puzzle state ─────────────────────────────────────────
    private int     size;
    private int[][] grid;           // -1=black, 0-4=numbered, 5=empty
    private boolean[][] solution;   // full solution bulb map from backtracking

    // ── animation state ──────────────────────────────────────
    private GameBoard   board;     // live board shown to user
    private List<Point> moveQueue; // solution bulbs in order
    private javax.swing.Timer revealTimer;
    private boolean     solved;
    private BoardPanel3 canvas;

    // ── DP memo: set of "state keys" known to be unsolvable ──
    private Set<String> failedStates;
    private int[][]     sharedPuzzle;

    public Game3_DPBacktracking(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 3 – DP + Backtracking (Computer)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel info = new JLabel("Algorithm: DP + Backtracking (exact)  |  Computer plays automatically");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(new Color(0, 100, 60));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.add(info);
        root.add(top, BorderLayout.NORTH);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(e -> loadGame(PuzzleDatabase.getRandomPuzzle()));
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bot.add(newGame);
        root.add(bot, BorderLayout.SOUTH);

        canvas = new BoardPanel3();
        root.add(canvas, BorderLayout.CENTER);
        add(root);

        loadGame(sharedPuzzle);
        pack();
    }

    // ────────────────────────────────────────────────────────
    private void loadGame(int[][] puzzle) {
        if (revealTimer != null) revealTimer.stop();
        solved = false;

        size = puzzle.length;
        // deep copy for solver
        grid = new int[size][size];
        for (int i = 0; i < size; i++) System.arraycopy(puzzle[i], 0, grid[i], 0, size);

        board = new GameBoard(puzzle);          // live display board
        solution = new boolean[size][size];
        failedStates = new HashSet<>();

        canvas.setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));
        pack();
        canvas.repaint();

        System.out.println("[G3] Solving with DP+Backtracking…");
        long t0 = System.currentTimeMillis();

        // Collect ordered list of empty cells to try
        List<Point> emptyCells = new ArrayList<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (grid[r][c] == 5) emptyCells.add(new Point(r, c));

        boolean[][] bulbs = new boolean[size][size];
        boolean found = backtrack(emptyCells, 0, bulbs);
        System.out.printf("[G3] Solved=%b  in %d ms%n", found, System.currentTimeMillis() - t0);

        if (!found) {
            JOptionPane.showMessageDialog(this, "No valid solution exists for this puzzle.",
                    "Game 3", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build move queue (solution bulbs in row-major order)
        moveQueue = new ArrayList<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (solution[r][c]) moveQueue.add(new Point(r, c));

        solved = true;
        revealTimer = new javax.swing.Timer(REVEAL_DELAY, e -> revealNextBulb());
        revealTimer.start();
    }

    private void revealNextBulb() {
        if (moveQueue.isEmpty()) {
            revealTimer.stop();
            System.out.println("[G3] All bulbs revealed!");
            JOptionPane.showMessageDialog(this, "Puzzle Solved! ✓", "Game 3", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Point p = moveQueue.remove(0);
        board.placeBulb(p.x, p.y);
        updateIllumination();
        canvas.repaint();
        System.out.printf("[G3] Revealed bulb at (%d,%d)%n", p.x, p.y);
    }

    // ── DP + Backtracking solver ─────────────────────────────

    /**
     * Recursive backtracking over emptyCells list.
     * At index idx, decide whether to place a bulb or not.
     * Uses a bitmask DP cache (failedStates) to prune repeated failures.
     */
    private boolean backtrack(List<Point> cells, int idx, boolean[][] bulbs) {
        // Pruning: check constraint violations so far
        if (violates(bulbs)) return false;

        if (idx == cells.size()) {
            // Base case: check if all cells are properly lit and all numbers satisfied
            return checkComplete(bulbs);
        }

        // DP memoisation key: partial bulb bitmask up to idx
        String key = stateKey(bulbs, idx);
        if (failedStates.contains(key)) return false;

        Point p = cells.get(idx);

        // Try placing a bulb here
        if (canPlace(p.x, p.y, bulbs)) {
            bulbs[p.x][p.y] = true;
            if (backtrack(cells, idx + 1, bulbs)) {
                // copy solution
                for (int r = 0; r < size; r++) System.arraycopy(bulbs[r], 0, solution[r], 0, size);
                return true;
            }
            bulbs[p.x][p.y] = false;
        }

        // Try skipping this cell
        if (backtrack(cells, idx + 1, bulbs)) {
            for (int r = 0; r < size; r++) System.arraycopy(bulbs[r], 0, solution[r], 0, size);
            return true;
        }

        // Neither worked – memoize failure
        failedStates.add(key);
        return false;
    }

    /** State key = partial bitmask of bulb positions up to cell index idx. */
    private String stateKey(boolean[][] bulbs, int idx) {
        // Use a compact string over the first idx empty cells' bulb bits
        StringBuilder sb = new StringBuilder(idx + 4);
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (grid[r][c] == 5) sb.append(bulbs[r][c] ? '1' : '0');
        sb.append('@').append(idx);
        return sb.toString();
    }

    private boolean canPlace(int r, int c, boolean[][] bulbs) {
        if (grid[r][c] != 5) return false;
        if (bulbs[r][c])     return false;
        // no visible existing bulb in LoS
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBounds(nr, nc)) {
                int v = grid[nr][nc];
                if (v == -1 || (v >= 0 && v <= 4)) break; // wall
                if (bulbs[nr][nc]) return false;           // sees another bulb
                nr += d[0]; nc += d[1];
            }
        }
        // won't violate adjacent numbers
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (inBounds(nr, nc) && grid[nr][nc] >= 0 && grid[nr][nc] <= 4) {
                int cnt = 0;
                for (int[] d2 : dirs) {
                    int er = nr + d2[0], ec = nc + d2[1];
                    if (inBounds(er, ec) && bulbs[er][ec]) cnt++;
                }
                if (cnt + 1 > grid[nr][nc]) return false;
            }
        }
        return true;
    }

    private boolean violates(boolean[][] bulbs) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                // Over-lit numbered cell
                if (grid[r][c] >= 0 && grid[r][c] <= 4) {
                    int cnt = 0;
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        if (inBounds(nr, nc) && bulbs[nr][nc]) cnt++;
                    }
                    if (cnt > grid[r][c]) return true;
                }
                // Bulb sees another bulb
                if (grid[r][c] == 5 && bulbs[r][c]) {
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        while (inBounds(nr, nc)) {
                            int v = grid[nr][nc];
                            if (v == -1 || (v >= 0 && v <= 4)) break;
                            if (bulbs[nr][nc]) return true;
                            nr += d[0]; nc += d[1];
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean checkComplete(boolean[][] bulbs) {
        // Every numbered cell must have exact count
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (grid[r][c] >= 0 && grid[r][c] <= 4) {
                    int cnt = 0;
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        if (inBounds(nr, nc) && bulbs[nr][nc]) cnt++;
                    }
                    if (cnt != grid[r][c]) return false;
                }
        // Every empty cell must be lit
        boolean[][] lit = computeLit(bulbs);
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (grid[r][c] == 5 && !lit[r][c]) return false;
        return true;
    }

    private boolean[][] computeLit(boolean[][] bulbs) {
        boolean[][] lit = new boolean[size][size];
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (bulbs[r][c]) {
                    lit[r][c] = true;
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        while (inBounds(nr, nc)) {
                            int v = grid[nr][nc];
                            if (v == -1 || (v >= 0 && v <= 4)) break;
                            lit[nr][nc] = true;
                            nr += d[0]; nc += d[1];
                        }
                    }
                }
        return lit;
    }

    private boolean inBounds(int r, int c) { return r >= 0 && r < size && c >= 0 && c < size; }

    // ── illumination on live display board ───────────────────
    private void updateIllumination() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY) {
                    board.setLit(r, c, board.hasBulb(r, c));
                    board.setBlocked(r, c, false);
                }
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.hasBulb(r, c)) propagate(r, c);
    }

    private void propagate(int r, int c) {
        board.setLit(r, c, true);
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (board.isValidCell(nr, nc)) {
                CellType t = board.getCellType(nr, nc);
                if (t == CellType.BLACK || t == CellType.NUMBERED) break;
                board.setLit(nr, nc, true);
                board.setBlocked(nr, nc, true);
                nr += d[0]; nc += d[1];
            }
        }
    }

    // ── rendering ────────────────────────────────────────────
    private class BoardPanel3 extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (board == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int sz = board.getSize();
            for (int row = 0; row < sz; row++) {
                for (int col = 0; col < sz; col++) {
                    int x = col * CELL_SIZE, y = row * CELL_SIZE;
                    CellType ct = board.getCellType(row, col);
                    if (ct == CellType.BLACK) {
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (ct == CellType.NUMBERED) {
                        g2.setColor(new Color(30, 30, 60));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 22));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (CELL_SIZE - fm.stringWidth(num)) / 2,
                                      y + (CELL_SIZE + fm.getAscent()) / 2 - 2);
                    } else {
                        g2.setColor(board.isLit(row, col) ? new Color(200, 255, 200) : new Color(245, 245, 245));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        if (board.hasBulb(row, col)) {
                            g2.setColor(new Color(50, 180, 50));
                            g2.fillOval(x + 10, y + 10, 40, 40);
                            g2.setColor(new Color(160, 255, 160));
                            g2.fillOval(x + 18, y + 18, 24, 24);
                        }
                    }
                    g2.setColor(Color.GRAY);
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
