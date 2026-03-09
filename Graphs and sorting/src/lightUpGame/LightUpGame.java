package lightUpGame;

import javax.swing.*;
import java.awt.*;

public class LightUpGame extends JFrame {
    private static final int CELL_SIZE = 60;
    private GameBoard board;
    private GamePanel panel;
    private JPanel mainPanel;
    
    public LightUpGame() {
        setTitle("Light Up Akari - Turn Based");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        mainPanel = new JPanel(new BorderLayout());
        
        // Load a random puzzle
        loadNewGame();
        
        // Add New Game button
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> loadNewGame());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(newGameButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void loadNewGame() {
        // Remove old panel if exists
        if (panel != null) {
            mainPanel.remove(panel);
        }
        
        // Load random puzzle from database
        board = new GameBoard(PuzzleDatabase.getRandomPuzzle());
        panel = new GamePanel(board);
        
        mainPanel.add(panel, BorderLayout.CENTER);
        
        // Revalidate and repaint
        pack();
        revalidate();
        repaint();
        
        System.out.println("\n=== NEW GAME STARTED ===");
        System.out.println("Grid Size: " + board.getSize() + "x" + board.getSize());
    }
}
