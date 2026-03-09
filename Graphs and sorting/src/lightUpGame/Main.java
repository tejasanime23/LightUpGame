package lightUpGame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LightUpGame game = new LightUpGame();
            game.setVisible(true);
        });
    }
}