package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

// ============================================================
//  GAME 2 – Divide & Conquer  (Computer-Only)
// ============================================================
//  Directly uses the existing D&C solver logic (ALGO1/ALGO2)
//  from AlgorithmSolver.java but with NO user-click interaction.
//  A Swing Timer fires every 700 ms to trigger a computer move.
// ============================================================
public class Game2_DivideConquer extends JFrame {

    private static final int CELL_SIZE  = 60;
    private static final int STEP_DELAY = 700;

    private GameBoard        board;
    private AlgorithmSolver  solver;
    private BoardPanel2      canvas;
    private JLabel           statusLabel;
    private javax.swing.Timer stepTimer;
    private boolean          done = false;
    private JLabel           stepLabel;
    private int[][]          sharedPuzzle;

    public Game2_DivideConquer(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 2 – Divide & Conquer (Computer)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // info label removed

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

        canvas = new BoardPanel2();
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
        if (stepTimer != null) stepTimer.stop();
        done  = false;
        board  = new GameBoard(puzzle);
        solver = new AlgorithmSolver(board);
        // Randomly pick ALGO1 or ALGO2 each game
        AlgorithmSolver.AlgoType algo = Math.random() < 0.5
                ? AlgorithmSolver.AlgoType.ALGO1
                : AlgorithmSolver.AlgoType.ALGO2;
        solver.setAlgorithm(algo);
        solver.resetSteps();
        if (statusLabel != null) statusLabel.setText(" ");
        if (stepLabel != null) stepLabel.setText("Steps: 0");
        System.out.println("[G2] New game – using " + algo);

        int sz = board.getSize();
        canvas.setPreferredSize(new Dimension(sz * CELL_SIZE, sz * CELL_SIZE));
        pack();
        canvas.repaint();

        stepTimer = new javax.swing.Timer(STEP_DELAY, e -> computerStep());
        stepTimer.start();
    }

    private void computerStep() {
        if (done) { stepTimer.stop(); return; }

        System.out.println("[G2] Computer thinking…");
        Point move = solver.findOptimalBulbPlacement();

        if (move == null) {
            done = true;
            stepTimer.stop();
            String msg = solver.isGameComplete() ? "Puzzle Solved! ✓" : "Computer failed to solve! ❌";
            System.out.println("[G2] " + msg);
            statusLabel.setText(msg);
            return;
        }

        System.out.printf("[G2] Placing bulb at (%d,%d)%n", move.x, move.y);
        board.placeBulb(move.x, move.y);
        solver.updateAfterBulbPlacement(move.x, move.y);
        stepLabel.setText("Steps: " + solver.getSteps());
        canvas.repaint();

        if (solver.isGameComplete()) {
            done = true;
            stepTimer.stop();
            System.out.println("[G2] Puzzle Solved!");
            statusLabel.setText("Puzzle Solved! ✓");
        }
    }

    // ── rendering ────────────────────────────────────────────
    private class BoardPanel2 extends JPanel {
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
                        g2.setColor(new Color(40, 40, 40));
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                        g2.setColor(Color.WHITE);
                        int fontSize = Math.max(10, currentCellSize / 2);
                        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (currentCellSize - fm.stringWidth(num)) / 2,
                                      y + (currentCellSize + fm.getAscent()) / 2 - 2);
                    } else {
                        g2.setColor(board.isLit(row, col) ? new Color(255, 230, 150) : Color.WHITE);
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                        if (board.hasBulb(row, col)) {
                            g2.setColor(new Color(255, 140, 0));
                            int padding = currentCellSize / 6;
                            int bulbSize = currentCellSize - padding * 2;
                            g2.fillOval(x + padding, y + padding, bulbSize, bulbSize);
                            g2.setColor(new Color(255, 230, 50));
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
