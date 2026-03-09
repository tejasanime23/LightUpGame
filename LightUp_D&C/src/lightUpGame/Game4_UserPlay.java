package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

// ============================================================
//  GAME 4 – User-Only Play
// ============================================================
//  Pure interactive game: ONLY the user places bulbs by
//  clicking empty cells.  No computer moves ever happen.
//   • Invalid placements are rejected silently (status bar).
//   • A "Puzzle Solved!" dialog appears on completion.
//   • A "New Game" button reloads a fresh puzzle.
// ============================================================
public class Game4_UserPlay extends JFrame {

    private static final int CELL_SIZE = 60;

    private GameBoard   board;
    private AlgorithmSolver solver; // used only for validation helpers
    private UserPanel   canvas;
    private JLabel      statusLabel;
    private int[][]     sharedPuzzle;

    public Game4_UserPlay(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 4 – User Play");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel info = new JLabel("Your turn! Click an empty cell to place a bulb.");
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));
        info.setForeground(new Color(100, 0, 140));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.add(info);
        root.add(top, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(180, 0, 0));

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(e -> loadGame(PuzzleDatabase.getRandomPuzzle()));

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        bot.add(newGame);
        bot.add(statusLabel);
        root.add(bot, BorderLayout.SOUTH);

        canvas = new UserPanel();
        root.add(canvas, BorderLayout.CENTER);

        add(root);
        loadGame(sharedPuzzle);
        pack();
    }

    // ── game lifecycle ───────────────────────────────────────
    private void loadGame(int[][] puzzle) {
        board  = new GameBoard(puzzle);
        solver = new AlgorithmSolver(board);

        int sz = board.getSize();
        canvas.setPreferredSize(new Dimension(sz * CELL_SIZE, sz * CELL_SIZE));
        pack();
        statusLabel.setText(" ");
        canvas.repaint();
        System.out.println("[G4] New user game – " + sz + "x" + sz);
    }

    // ── user click handler ───────────────────────────────────
    private void handleClick(int row, int col) {
        if (!board.isValidCell(row, col)) return;
        if (board.getCellType(row, col) != CellType.EMPTY) {
            statusLabel.setText("That cell is a wall – pick an empty cell.");
            return;
        }
        if (board.hasBulb(row, col)) {
            statusLabel.setText("There is already a bulb there!");
            return;
        }
        if (board.isBlocked(row, col)) {
            statusLabel.setText("That cell is blocked by existing light rays.");
            return;
        }
        if (!solver.isValidBulbPlacement(row, col)) {
            statusLabel.setText("Invalid placement – conflicts with a constraint or another bulb.");
            return;
        }

        statusLabel.setText("Bulb placed at (" + row + ", " + col + ")");
        board.placeBulb(row, col);
        solver.updateAfterBulbPlacement(row, col);
        canvas.repaint();
        System.out.printf("[G4] User placed bulb at (%d,%d)%n", row, col);

        if (solver.isGameComplete()) {
            System.out.println("[G4] Puzzle Solved by user!");
            JOptionPane.showMessageDialog(this, "🎉 You Solved It! Congratulations!",
                    "Game 4", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ── rendering + mouse ────────────────────────────────────
    private class UserPanel extends JPanel {

        UserPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = e.getY() / CELL_SIZE;
                    int col = e.getX() / CELL_SIZE;
                    handleClick(row, col);
                }
            });
        }

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
                        g2.setColor(new Color(50, 20, 80));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 22));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (CELL_SIZE - fm.stringWidth(num)) / 2,
                                      y + (CELL_SIZE + fm.getAscent()) / 2 - 2);
                    } else {
                        // empty cell
                        Color bg = board.isLit(row, col)
                                ? new Color(220, 210, 255)   // light purple = lit
                                : Color.WHITE;
                        g2.setColor(bg);
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                        if (board.hasBulb(row, col)) {
                            // Draw bulb icon
                            g2.setColor(new Color(160, 80, 220));
                            g2.fillOval(x + 10, y + 10, 40, 40);
                            g2.setColor(new Color(230, 180, 255));
                            g2.fillOval(x + 18, y + 18, 24, 24);
                        } else if (!board.isLit(row, col)) {
                            // Hover hint: draw faint circle when mouse is over unlit cell
                            // (static; actual hover needs mouse-move tracking)
                        }
                    }
                    g2.setColor(new Color(150, 120, 180));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
