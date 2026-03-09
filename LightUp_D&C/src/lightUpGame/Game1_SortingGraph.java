package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

// ============================================================
//  GAME 1 – Sorting + Graph (Computer-Only)
// ============================================================
//  Strategy (approximate – does NOT guarantee 100% solution):
//   1. Build a constraint graph: numbered cells = nodes; edges
//      connect numbered cells that share at least one reachable
//      empty neighbour (they "see" the same candidate square).
//   2. BFS over that graph to find the most-constrained region.
//   3. Sort all un-lit empty cells by a constraint priority
//      score: higher score = more constrained = place first.
//   4. Greedily place a bulb at the best candidate each step.
//  Because it is purely greedy it may fail to illuminate every
//  cell, but it never cheats / doesn't backtrack.
// ============================================================
public class Game1_SortingGraph extends JFrame {

    // ── UI / timing ─────────────────────────────────────────
    private static final int CELL_SIZE   = 60;
    private static final int STEP_DELAY  = 650; // ms between moves
    private static final Color COL_LIT   = new Color(255, 255, 180);
    private static final Color COL_BULB1 = new Color(255, 165, 0);
    private static final Color COL_BULB2 = new Color(255, 230, 50);

    // ── puzzle state ─────────────────────────────────────────
    private GameBoard   board;
    private int         size;
    private BoardPanel  canvas;
    private javax.swing.Timer stepTimer;
    private boolean     done = false;

    // ── solver state ─────────────────────────────────────────
    // Sorted queue rebuilt every step (greedy re-rank)
    private List<Point> sortedCandidates = new ArrayList<>();
    // Shared puzzle passed from Main (same for all 4 games on launch)
    private int[][] sharedPuzzle;

    // ────────────────────────────────────────────────────────
    public Game1_SortingGraph(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 1 – Sorting + Graph (Computer)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // info label
        JLabel info = new JLabel("Algorithm: Sorting + Graph  |  Computer plays automatically");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(new Color(60, 60, 140));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.add(info);
        root.add(top, BorderLayout.NORTH);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(e -> loadGame(PuzzleDatabase.getRandomPuzzle()));
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bot.add(newGame);
        root.add(bot, BorderLayout.SOUTH);

        canvas = new BoardPanel();
        root.add(canvas, BorderLayout.CENTER);

        add(root);
        loadGame(sharedPuzzle);
        pack();
    }

    // ── game lifecycle ───────────────────────────────────────
    /** Load with a specific puzzle (used on first launch for shared puzzle). */
    private void loadGame(int[][] puzzle) {
        if (stepTimer != null) stepTimer.stop();
        done  = false;
        board = new GameBoard(puzzle);
        size  = board.getSize();
        canvas.setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));
        pack();
        updateIllumination();
        canvas.repaint();

        System.out.println("[G1] New game loaded – " + size + "x" + size);

        stepTimer = new javax.swing.Timer(STEP_DELAY, e -> computerStep());
        stepTimer.start();
    }

    // ── one greedy step ──────────────────────────────────────
    private void computerStep() {
        if (done) { stepTimer.stop(); return; }

        // rebuild sorted candidate list each step
        sortedCandidates = buildSortedCandidates();

        if (sortedCandidates.isEmpty()) {
            done = true;
            stepTimer.stop();
            String msg = isComplete() ? "Puzzle Solved! ✓" : "No more moves – puzzle incomplete.";
            System.out.println("[G1] " + msg);
            JOptionPane.showMessageDialog(this, msg, "Game 1", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Point best = sortedCandidates.get(0);
        System.out.printf("[G1] Placing bulb at (%d,%d) score=%d%n",
                best.x, best.y, priorityScore(best.x, best.y));
        board.placeBulb(best.x, best.y);
        updateIllumination();
        canvas.repaint();

        if (isComplete()) {
            done = true;
            stepTimer.stop();
            System.out.println("[G1] Puzzle Solved!");
            JOptionPane.showMessageDialog(this, "Puzzle Solved! ✓", "Game 1", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ── Sorting + Graph core logic ───────────────────────────

    /**
     * Build a graph where each numbered cell is a node.
     * Two numbered cells are connected if they share a
     * reachable empty neighbour (BFS over empty cells).
     * Then sort all EMPTY, un-lit, un-bulbed cells by their
     * priority score (higher = more constrained).
     */
    private List<Point> buildSortedCandidates() {
        // Step 1 – collect candidate empty cells
        List<Point> candidates = new ArrayList<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY
                        && !board.hasBulb(r, c)
                        && !board.isLit(r, c)
                        && !board.isBlocked(r, c)
                        && canPlaceBulb(r, c))
                    candidates.add(new Point(r, c));

        // Step 2 – score each candidate using constraint-graph info
        candidates.sort((a, b) -> priorityScore(b.x, b.y) - priorityScore(a.x, a.y));
        return candidates;
    }

    /**
     * Priority score for a candidate cell (row, col).
     * Considers:
     *  - # of numbered cells that can see it (graph degree)
     *  - # of empty cells visible to it in line-of-sight
     *    (lower visibility = less "powerful" = higher priority to place now)
     *  - BFS neighbourhood density of unnlighted cells
     */
    private int priorityScore(int row, int col) {
        int score = 0;

        // Penalty for large illumination reach (we prefer tight placements)
        int reach = lineOfSightCells(row, col);
        score -= reach;               // fewer reachable = higher priority

        // Bonus for adjacent numbered constraints
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            if (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.NUMBERED) {
                int need = board.getNumberValue(nr, nc)
                         - countBulbsAround(nr, nc);
                score += need * 10;  // high need = high priority
            }
        }

        // BFS: count dark (unlit, non-bulb) cells reachable within 2 hops
        // More dark neighbours = we should illuminate this area sooner
        int darkNeighbours = bfsDarkCount(row, col, 2);
        score += darkNeighbours;

        return score;
    }

    /** Count empty cells visible to (r,c) in 4 directions (line-of-sight). */
    private int lineOfSightCells(int r, int c) {
        int cnt = 0;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (board.isValidCell(nr, nc)) {
                CellType t = board.getCellType(nr, nc);
                if (t == CellType.BLACK || t == CellType.NUMBERED) break;
                cnt++;
                nr += d[0]; nc += d[1];
            }
        }
        return cnt;
    }

    /** BFS: count unlit, non-bulb empty cells within 'maxHops' hops. */
    private int bfsDarkCount(int startR, int startC, int maxHops) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        boolean[][] visited = new boolean[size][size];
        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startR, startC, 0});
        visited[startR][startC] = true;
        int count = 0;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1], hops = cur[2];
            if (hops > maxHops) continue;
            if (!board.isLit(r, c) && !board.hasBulb(r, c)) count++;
            if (hops == maxHops) continue;
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (board.isValidCell(nr, nc) && !visited[nr][nc]
                        && board.getCellType(nr, nc) == CellType.EMPTY) {
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc, hops + 1});
                }
            }
        }
        return count;
    }

    // ── constraint helpers ───────────────────────────────────
    private boolean canPlaceBulb(int r, int c) {
        if (board.getCellType(r, c) != CellType.EMPTY) return false;
        if (board.hasBulb(r, c) || board.isBlocked(r, c))  return false;
        // no visible bulb in line of sight
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (board.isValidCell(nr, nc)) {
                CellType t = board.getCellType(nr, nc);
                if (t == CellType.BLACK || t == CellType.NUMBERED) break;
                if (board.hasBulb(nr, nc)) return false;
                nr += d[0]; nc += d[1];
            }
        }
        // won't exceed any adjacent number
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.NUMBERED) {
                if (countBulbsAround(nr, nc) + 1 > board.getNumberValue(nr, nc)) return false;
            }
        }
        return true;
    }

    private int countBulbsAround(int r, int c) {
        int cnt = 0;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (board.isValidCell(nr, nc) && board.hasBulb(nr, nc)) cnt++;
        }
        return cnt;
    }

    private void updateIllumination() {
        // reset
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY) {
                    board.setLit(r, c, board.hasBulb(r, c));
                    board.setBlocked(r, c, false);
                }
        // propagate
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

    private boolean isComplete() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.EMPTY && !board.isLit(r, c))
                    return false;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.getCellType(r, c) == CellType.NUMBERED)
                    if (countBulbsAround(r, c) != board.getNumberValue(r, c))
                        return false;
        return true;
    }

    // ── rendering ────────────────────────────────────────────
    private class BoardPanel extends JPanel {
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
                        g2.setColor(new Color(40, 40, 40));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 22));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (CELL_SIZE - fm.stringWidth(num)) / 2,
                                      y + (CELL_SIZE + fm.getAscent()) / 2 - 2);
                    } else {
                        g2.setColor(board.isLit(row, col) ? COL_LIT : Color.WHITE);
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        if (board.hasBulb(row, col)) {
                            g2.setColor(COL_BULB1);
                            g2.fillOval(x + 10, y + 10, 40, 40);
                            g2.setColor(COL_BULB2);
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
