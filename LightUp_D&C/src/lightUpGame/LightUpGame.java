package lightUpGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LightUpGame extends JFrame {

    private GameBoard board;
    private GamePanel panel;
    private JPanel mainPanel;
    private JComboBox<String> algoCombo;

    private static final String[] ALGO_LABELS = {
        "Algorithm 1 ",
        "Algorithm 2"
    };

    public LightUpGame() {
        setTitle("Light Up / Akari");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ── Top bar: algorithm selector ───────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        topBar.setBorder(BorderFactory.createTitledBorder("Select Algorithm"));

        algoCombo = new JComboBox<>(ALGO_LABELS);
        algoCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        algoCombo.setFocusable(false);
        topBar.add(algoCombo);

        mainPanel.add(topBar, BorderLayout.NORTH);

        // ── Bottom bar: new game button ───────────────────────────────────────
        JButton newGameBtn = new JButton("New Game");
        newGameBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        newGameBtn.addActionListener(e -> loadNewGame());

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomBar.add(newGameBtn);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        loadNewGame();

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private AlgorithmSolver.AlgoType selectedAlgo() {
        return algoCombo.getSelectedIndex() == 0
                ? AlgorithmSolver.AlgoType.ALGO1
                : AlgorithmSolver.AlgoType.ALGO2;
    }

    private void loadNewGame() {
        if (panel != null) mainPanel.remove(panel);

        AlgorithmSolver.AlgoType algo = selectedAlgo();
        board = new GameBoard(PuzzleDatabase.getRandomPuzzle());
        panel = new GamePanel(board, algo);

        mainPanel.add(panel, BorderLayout.CENTER);
        pack();
        revalidate();
        repaint();

        System.out.println("\n=== NEW GAME [" + algo + "] ===");
        System.out.println("Grid: " + board.getSize() + "×" + board.getSize());
    }
}
