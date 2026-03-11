package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

// ============================================================
//  GAME 5 – User-Only Play
// ============================================================
//  Pure interactive game: ONLY the user places bulbs by
//  clicking empty cells.  No computer moves ever happen.
//   • Invalid placements are rejected silently (status bar).
//   • A "Puzzle Solved!" dialog appears on completion.
//   • A "New Game" button reloads a fresh puzzle.
// ============================================================
public class Game5_UserPlay extends JFrame {

    private static final int CELL_SIZE = 60;

    private GameBoard   board;
    private AlgorithmSolver solver; // used only for validation helpers
    private UserPanel   canvas;
    private JLabel      statusLabel;
    private int[][]     sharedPuzzle;

    public Game5_UserPlay(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        setTitle("Game 5 – User Play");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // info label removed

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(180, 0, 0));
        JPanel bot = new JPanel(new BorderLayout());
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bot.add(statusLabel, BorderLayout.CENTER);
        root.add(bot, BorderLayout.SOUTH);

        canvas = new UserPanel();
        root.add(canvas, BorderLayout.CENTER);

        add(root);
        loadGame(sharedPuzzle);
        pack();
    }

    // ── game lifecycle ───────────────────────────────────────
    public void loadSharedGame(int[][] puzzle) {
        this.sharedPuzzle = puzzle;
        loadGame(this.sharedPuzzle);
    }

    private void loadGame(int[][] puzzle) {
        board  = new GameBoard(puzzle);
        solver = new AlgorithmSolver(board);

        int sz = board.getSize();
        canvas.setPreferredSize(new Dimension(sz * CELL_SIZE, sz * CELL_SIZE));
        pack();
        statusLabel.setText(" ");
        canvas.repaint();
        System.out.println("[G5] New user game – " + sz + "x" + sz);
    }

    // ── user click handler ───────────────────────────────────
    private void handleClick(int row, int col) {
        if (!board.isValidCell(row, col)) return;
        if (board.getCellType(row, col) != CellType.EMPTY) {
            statusLabel.setText("That cell is a wall – pick an empty cell.");
            return;
        }
        if (board.hasBulb(row, col)) {
            board.removeBulb(row, col);
            solver.updateAfterBulbPlacement(row, col); // Recomputes illumination for all
            statusLabel.setText("Bulb removed at (" + row + ", " + col + ")");
            canvas.repaint();
            System.out.printf("[G5] User removed bulb at (%d,%d)%n", row, col);
            return;
        }

        if (board.isBlocked(row, col)) {
            // Still allow placing it so user can "make an invalid move"
            statusLabel.setText("Warning: Bulb placed in the light of another! (" + row + ", " + col + ")");
        } else {
            statusLabel.setText("Bulb placed at (" + row + ", " + col + ")");
        }

        board.placeBulb(row, col);
        solver.updateAfterBulbPlacement(row, col);
        canvas.repaint();
        System.out.printf("[G5] User placed bulb at (%d,%d)%n", row, col);

        if (solver.isGameComplete()) {
            System.out.println("[G5] Puzzle Solved by user!");
            statusLabel.setText("🎉 You Solved It! Congratulations!");
        }
    }

    // Helper: Checks if a placed bulb is adjacent to a numbered block that has TOO MANY bulbs
    private boolean isBulbViolatingNumberConstraint(int r, int c) {
        if (!board.hasBulb(r, c)) return false;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (board.isValidCell(nr, nc) && board.getCellType(nr, nc) == CellType.NUMBERED) {
                // Count placed bulbs around THIS numbered block
                int count = 0;
                for (int[] d2 : dirs) {
                    int er = nr + d2[0], ec = nc + d2[1];
                    if (board.isValidCell(er, ec) && board.hasBulb(er, ec)) count++;
                }
                if (count > board.getNumberValue(nr, nc)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── rendering + mouse ────────────────────────────────────
    private class UserPanel extends JPanel {

        UserPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int sz = board.getSize();
                    int currentCellSize = Math.min(getWidth() / sz, getHeight() / sz);
                    int xOffset = (getWidth() - (sz * currentCellSize)) / 2;
                    int yOffset = (getHeight() - (sz * currentCellSize)) / 2;
                    
                    int col = (e.getX() - xOffset) / currentCellSize;
                    int row = (e.getY() - yOffset) / currentCellSize;
                    if (row >= 0 && row < sz && col >= 0 && col < sz) {
                        handleClick(row, col);
                    }
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
                        g2.setColor(new Color(50, 20, 80));
                        g2.fillRect(x, y, currentCellSize, currentCellSize);
                        g2.setColor(Color.WHITE);
                        int fontSize = Math.max(10, currentCellSize / 2);
                        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                        String num = String.valueOf(board.getNumberValue(row, col));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num, x + (currentCellSize - fm.stringWidth(num)) / 2,
                                      y + (currentCellSize + fm.getAscent()) / 2 - 2);
                    } else {
                        // empty cell
                        Color bg = board.isLit(row, col)
                                ? new Color(220, 210, 255)   // light purple = lit
                                : Color.WHITE;
                        g2.setColor(bg);
                        g2.fillRect(x, y, currentCellSize, currentCellSize);

                        if (board.hasBulb(row, col)) {
                            // Check if this bulb violates a number constraint
                            boolean isInvalid = isBulbViolatingNumberConstraint(row, col);
                            
                            // Base color (red if invalid, purple if okay)
                            Color baseColor = isInvalid ? new Color(220, 60, 60) : new Color(160, 80, 220);
                            Color innerColor = isInvalid ? new Color(255, 120, 120) : new Color(230, 180, 255);

                            // Draw bulb icon
                            g2.setColor(baseColor);
                            int padding = currentCellSize / 6;
                            int bulbSize = currentCellSize - padding * 2;
                            g2.fillOval(x + padding, y + padding, bulbSize, bulbSize);
                            
                            g2.setColor(innerColor);
                            int innerPadding = currentCellSize / 3;
                            int innerSize = currentCellSize - innerPadding * 2;
                            g2.fillOval(x + innerPadding, y + innerPadding, innerSize, innerSize);
                        } else if (!board.isLit(row, col)) {
                            // Hover hint: draw faint circle when mouse is over unlit cell
                            // (static; actual hover needs mouse-move tracking)
                        }
                    }
                    g2.setColor(new Color(150, 120, 180));
                    g2.drawRect(x, y, currentCellSize, currentCellSize);
                }
            }
        }
    }
}
