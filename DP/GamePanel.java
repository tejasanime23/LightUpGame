package lightUpGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class GamePanel extends JPanel {
    private static final int CELL_SIZE = 60;
    private GameBoard board;
    private AlgorithmSolver solver;
    private JLabel statusLabel;

    public GamePanel(GameBoard board, AlgorithmSolver.AlgoType algoType, JLabel statusLabel) {
        this.board  = board;
        this.solver = new AlgorithmSolver(board);
        this.solver.setAlgorithm(algoType);
        this.statusLabel = statusLabel;

        int size = board.getSize();
        setPreferredSize(new Dimension(size * CELL_SIZE, size * CELL_SIZE));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int size = board.getSize();
                int currentCellSize = Math.min(getWidth() / size, getHeight() / size);
                int xOffset = (getWidth() - (size * currentCellSize)) / 2;
                int yOffset = (getHeight() - (size * currentCellSize)) / 2;
                
                int col = (e.getX() - xOffset) / currentCellSize;
                int row = (e.getY() - yOffset) / currentCellSize;
                if (row >= 0 && row < size && col >= 0 && col < size) {
                    handlePlayerMove(row, col);
                }
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
            if (statusLabel != null) statusLabel.setText("Puzzle Solved!");
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
                if (statusLabel != null) statusLabel.setText("Puzzle Solved!");
            }
        } else {
            System.out.println("Computer: no valid move found.");
            if (statusLabel != null && !solver.isGameComplete()) statusLabel.setText("Computer failed to solve! ❌");
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
        int currentCellSize = Math.min(getWidth() / size, getHeight() / size);
        int xOffset = (getWidth() - (size * currentCellSize)) / 2;
        int yOffset = (getHeight() - (size * currentCellSize)) / 2;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = xOffset + col * currentCellSize;
                int y = yOffset + row * currentCellSize;

                if (board.getCellType(row, col) == CellType.BLACK) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(x, y, currentCellSize, currentCellSize);

                } else if (board.getCellType(row, col) == CellType.NUMBERED) {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(x, y, currentCellSize, currentCellSize);
                    g2.setColor(Color.WHITE);
                    int fontSize = Math.max(10, currentCellSize / 2);
                    g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                    String num = String.valueOf(board.getNumberValue(row, col));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(num,
                        x + (currentCellSize - fm.stringWidth(num)) / 2,
                        y + (currentCellSize + fm.getAscent()) / 2 - 2);

                } else {
                    g2.setColor(board.isLit(row, col) ? new Color(255, 255, 200) : Color.WHITE);
                    g2.fillRect(x, y, currentCellSize, currentCellSize);
                    if (board.hasBulb(row, col)) {
                        g2.setColor(Color.ORANGE);
                        int padding = currentCellSize / 4;
                        g2.fillOval(x + padding, y + padding, currentCellSize - padding*2, currentCellSize - padding*2);
                        g2.setColor(Color.YELLOW);
                        int innerPadding = currentCellSize / 3;
                        g2.fillOval(x + innerPadding, y + innerPadding, currentCellSize - innerPadding*2, currentCellSize - innerPadding*2);
                    }
                }
                g2.setColor(Color.GRAY);
                g2.drawRect(x, y, currentCellSize, currentCellSize);
            }
        }
    }
}