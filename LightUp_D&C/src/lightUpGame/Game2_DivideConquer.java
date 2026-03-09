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
    private javax.swing.Timer stepTimer;
    private boolean          done = false;
    private int[][]          sharedPuzzle;

    public Game2_DivideConquer(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 2 – Divide & Conquer (Computer)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel info = new JLabel("Algorithm: Divide & Conquer  |  Computer plays automatically");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(new Color(140, 60, 0));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.add(info);
        root.add(top, BorderLayout.NORTH);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(e -> loadGame(PuzzleDatabase.getRandomPuzzle()));
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bot.add(newGame);
        root.add(bot, BorderLayout.SOUTH);

        canvas = new BoardPanel2();
        root.add(canvas, BorderLayout.CENTER);

        add(root);
        loadGame(sharedPuzzle);
        pack();
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
            String msg = solver.isGameComplete() ? "Puzzle Solved! ✓" : "No valid move found – puzzle incomplete.";
            System.out.println("[G2] " + msg);
            JOptionPane.showMessageDialog(this, msg, "Game 2", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        System.out.printf("[G2] Placing bulb at (%d,%d)%n", move.x, move.y);
        board.placeBulb(move.x, move.y);
        solver.updateAfterBulbPlacement(move.x, move.y);
        canvas.repaint();

        if (solver.isGameComplete()) {
            done = true;
            stepTimer.stop();
            System.out.println("[G2] Puzzle Solved!");
            JOptionPane.showMessageDialog(this, "Puzzle Solved! ✓", "Game 2", JOptionPane.INFORMATION_MESSAGE);
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
                        g2.setColor(board.isLit(row, col) ? new Color(255, 230, 150) : Color.WHITE);
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        if (board.hasBulb(row, col)) {
                            g2.setColor(new Color(255, 140, 0));
                            g2.fillOval(x + 10, y + 10, 40, 40);
                            g2.setColor(new Color(255, 230, 50));
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
