package lightUpGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class GamePanel extends JPanel {
    private static final int CELL_SIZE = 60;
    private GameBoard board;
    private AlgorithmSolver solver;
    
    public GamePanel(GameBoard board) {
        this.board = board;
        this.solver = new AlgorithmSolver(board);
        
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
        if (!board.isValidCell(row, col)) {
            System.out.println("ERROR: Invalid cell selection!");
            return;
        }
        
        if (board.getCellType(row, col) != CellType.EMPTY) {
            System.out.println("ERROR: Can only place bulbs on empty cells!");
            return;
        }
        
        if (board.hasBulb(row, col)) {
            System.out.println("ERROR: Bulb already placed here!");
            return;
        }
        
        if (board.isBlocked(row, col)) {
            System.out.println("ERROR: Cell is blocked by visibility constraint!");
            return;
        }
        
        // Check constraint violations
        if (!solver.isValidBulbPlacement(row, col)) {
            System.out.println("ERROR: Placement violates numbered cell constraint!");
            return;
        }
        
        // Valid move - place player's bulb
        System.out.println("Player placed bulb at (" + row + ", " + col + ")");
        board.placeBulb(row, col);
        solver.updateAfterBulbPlacement(row, col);
        repaint();
        
        // Check if game is complete
        if (solver.isGameComplete()) {
            System.out.println("GAME COMPLETE! All cells lit and constraints satisfied!");
            JOptionPane.showMessageDialog(this, "Puzzle Solved!");
            return;
        }
        
        // Computer's turn
        Timer timer = new Timer(500, e -> {
            makeComputerMove();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void makeComputerMove() {
        System.out.println("\n=== Computer's Turn ===");
        
        // Count bulbs before
        int bulbsBefore = countTotalBulbs();
        
        Point computerMove = solver.findOptimalBulbPlacement();
        
        if (computerMove != null) {
            System.out.println("Computer strategically placed bulb at (" + computerMove.x + ", " + computerMove.y + ")");
            board.placeBulb(computerMove.x, computerMove.y);
            solver.updateAfterBulbPlacement(computerMove.x, computerMove.y);
            
            // Count bulbs after (includes forced placements)
            int bulbsAfter = countTotalBulbs();
            int forcedPlacements = bulbsAfter - bulbsBefore - 1;
            
            if (forcedPlacements > 0) {
                System.out.println("  → Triggered " + forcedPlacements + " forced constraint placement(s)");
            }
            
            repaint();
            
            if (solver.isGameComplete()) {
                System.out.println("\n🎉 GAME COMPLETE! All cells lit and constraints satisfied!");
                JOptionPane.showMessageDialog(this, "Puzzle Solved!");
            }
        } else {
            System.out.println("Computer has no valid moves remaining.");
        }
        System.out.println("=== Computer's Turn Complete ===\n");
    }
    
    private int countTotalBulbs() {
        int count = 0;
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.hasBulb(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int size = board.getSize();
        
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;
                
                // Draw cell background
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
                    int textX = x + (CELL_SIZE - fm.stringWidth(num)) / 2;
                    int textY = y + (CELL_SIZE + fm.getAscent()) / 2 - 2;
                    g2.drawString(num, textX, textY);
                } else {
                    // Empty cell
                    if (board.isLit(row, col)) {
                        g2.setColor(new Color(255, 255, 200));
                    } else {
                        g2.setColor(Color.WHITE);
                    }
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    
                    // Draw bulb if present
                    if (board.hasBulb(row, col)) {
                        g2.setColor(Color.ORANGE);
                        g2.fillOval(x + 15, y + 15, 30, 30);
                        g2.setColor(Color.YELLOW);
                        g2.fillOval(x + 20, y + 20, 20, 20);
                    }
                }
                
                // Draw grid lines
                g2.setColor(Color.GRAY);
                g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}