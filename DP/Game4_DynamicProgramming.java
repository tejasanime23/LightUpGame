package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Game4_DynamicProgramming extends JFrame {

    private static final int CELL_SIZE = 60;
    private static final int REVEAL_DELAY = 400;

    private int size;
    private int[][] grid;
    private GameBoard board;
    private BoardPanel4 canvas;
    private JLabel statusLabel;
    private JLabel stepLabel;
    private int steps;
    private int[][] sharedPuzzle;
    private javax.swing.Timer revealTimer;
    private List<Point> moveQueue;
// DP logic classes
    private static class State {
        long colIllum; // Bitmask of M bits
        long colNeedsIllum; // Bitmask of columns that MUST be lit from below
        String counts; // Encoded counts of numbered cells

        State(long colIllum, long colNeedsIllum, String counts) {
            this.colIllum = colIllum;
            this.colNeedsIllum = colNeedsIllum;
            this.counts = counts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State other = (State) o;
            return colIllum == other.colIllum && colNeedsIllum == other.colNeedsIllum && counts.equals(other.counts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(colIllum, colNeedsIllum, counts);
        }
    }

    private static class BackPointer {
        State parent;
        long placement; // Bitmask of bulbs in the row

        BackPointer(State parent, long placement) {
            this.parent = parent;
            this.placement = placement;
        }
    }

    public Game4_DynamicProgramming(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 4 – Dynamic Programming (Computer)");
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
        canvas = new BoardPanel4();
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
        if (revealTimer != null) revealTimer.stop();
        
        size = puzzle.length;
        grid = new int[size][size];
        for (int i = 0; i < size; i++) System.arraycopy(puzzle[i], 0, grid[i], 0, size);

        board = new GameBoard(puzzle);
        canvas.setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));
        pack();
        canvas.repaint();

        statusLabel.setText("Solving...");
        steps = 0;
        stepLabel.setText("Steps: 0");

        // Run solving in a background thread to keep UI responsive
        new Thread(() -> {
            long t0 = System.currentTimeMillis();
            boolean[][] sol = solveDP();
            long t1 = System.currentTimeMillis();
            
            SwingUtilities.invokeLater(() -> {
                System.out.printf("[G4] Dynamic Programming Solved in (Steps: %d)%n", steps);
                stepLabel.setText("Steps: " + steps);
                if (sol == null) {
                    statusLabel.setText("No valid solution found! ❌");
                } else {
                    statusLabel.setText(" ");
                    startAnimation(sol);
                }
            });
        }).start();
    }

    private boolean[][] solveDP() {
        // Preprocessing
        List<Point> numberedCells = new ArrayList<>();
        Map<Point, Integer> lastAdjRowMap = new HashMap<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (grid[r][c] >= 0 && grid[r][c] <= 4) {
                    Point p = new Point(r, c);
                    numberedCells.add(p);
                    // Last row with a neighbor is max(r, nr) where nr is a neighbor row
                    int maxR = r; 
                    if (r + 1 < size && grid[r + 1][c] == 5) {
                        maxR = r + 1;
                    }
                    lastAdjRowMap.put(p, maxR);
                }
            }
        }

  private void startAnimation(boolean[][] sol) {
        moveQueue = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (sol[r][c]) moveQueue.add(new Point(r, c));
            }
        }
        revealTimer = new javax.swing.Timer(REVEAL_DELAY, e -> {
            if (moveQueue.isEmpty()) {
                revealTimer.stop();
                statusLabel.setText("Puzzle Solved! ✓");
                return;
            }
            Point p = moveQueue.remove(0);
            board.placeBulb(p.x, p.y);
            updateDisplayIllumination();
            canvas.repaint();
        });
        revealTimer.start();
    }

    private void updateDisplayIllumination() {
        for (int r=0; r<size; r++) {
            for (int c=0; c<size; c++) {
                if (board.getCellType(r, c) == CellType.EMPTY) board.setLit(r, c, false);
            }
        }
        for (int r=0; r<size; r++) {
            for (int c=0; c<size; c++) {
                if (board.hasBulb(r, c)) {
                    board.setLit(r, c, true);
                    int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        while (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.EMPTY) {
                            board.setLit(nr, nc, true);
                            nr += d[0]; nc += d[1];
                        }
                    }
                }
            }
        }
    }

    private class BoardPanel4 extends JPanel {
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
