package lightUpGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class GamePanel extends JPanel {
    private static final int CELL_SIZE = 60;
    private GameBoard board;
    private AlgorithmSolver solver;

    public GamePanel(GameBoard board, AlgorithmSolver.AlgoType algoType) {
        this.board  = board;
        this.solver = new AlgorithmSolver(board);
        this.solver.setAlgorithm(algoType);

        int size = board.getSize();
        setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = e.getY() / CELL_SIZE;
                int col = e.getX() / CELL_SIZE;
                handlePlayerMove(row, col);
            }
        });
    }

    private void handlePlayerMove(int row, int col) {
        if (!board.isValidCell(row, col))                   return;
        if (board.getCellType(row, col) != CellType.EMPTY)  return;
        if (board.hasBulb(row, col))                        return;
        if (board.isBlocked(row, col))                      return;
        if (!solver.isValidBulbPlacement(row, col))         return;

        System.out.println("Player placed bulb at (" + row + ", " + col + ")");
        board.placeBulb(row, col);
        solver.updateAfterBulbPlacement(row, col);
        repaint();

        if (solver.isGameComplete()) {
            JOptionPane.showMessageDialog(this, "Puzzle Solved!");
            return;
        }

        Timer timer = new Timer(500, e -> makeComputerMove());
        timer.setRepeats(false);
        timer.start();
    }

    private void makeComputerMove() {
        System.out.println("\n=== Computer's Turn [" + solver.getAlgorithm() + "] ===");
        int before = countBulbs();

        Point move = solver.findOptimalBulbPlacement();
        if (move != null) {
            System.out.println("Computer placed bulb at (" + move.x + ", " + move.y + ")");
            board.placeBulb(move.x, move.y);
            solver.updateAfterBulbPlacement(move.x, move.y);

            int forced = countBulbs() - before - 1;
            if (forced > 0) System.out.println("  → " + forced + " forced placement(s)");

            repaint();
            if (solver.isGameComplete()) {
                System.out.println("GAME COMPLETE!");
                JOptionPane.showMessageDialog(this, "Puzzle Solved!");
            }
        } else {
            System.out.println("Computer: no valid move found.");
        }
        System.out.println("=== Computer's Turn Complete ===\n");
    }

    private int countBulbs() {
        int n = 0;
        for (int r = 0; r < board.getSize(); r++)
            for (int c = 0; c < board.getSize(); c++)
                if (board.hasBulb(r, c)) n++;
        return n;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = board.getSize();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = col * CELL_SIZE, y = row * CELL_SIZE;

                if (board.getCellType(row, col) == CellType.BLACK) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                } else if (board.getCellType(row, col) == CellType.NUMBERED) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 24));
                    String num = String.valueOf(board.getNumberValue(row, col));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(num,
                        x + (CELL_SIZE - fm.stringWidth(num)) / 2,
                        y + (CELL_SIZE + fm.getAscent()) / 2 - 2);

                } else {
                    g2.setColor(board.isLit(row, col) ? new Color(255, 255, 200) : Color.WHITE);
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    if (board.hasBulb(row, col)) {
                        g2.setColor(Color.ORANGE);
                        g2.fillOval(x + 15, y + 15, 30, 30);
                        g2.setColor(Color.YELLOW);
                        g2.fillOval(x + 20, y + 20, 20, 20);
                    }
                }
                g2.setColor(Color.GRAY);
                g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}
