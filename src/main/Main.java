package main;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {

    public static void main(String[] args) {

        // Mode selection
        Object[] modeOptions = {"Human vs Human", "Human vs AI"};
        int modeChoice = JOptionPane.showOptionDialog(
                null, "Select game mode:", "Chess",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, modeOptions, modeOptions[0]);

        GameMode mode = (modeChoice == 1) ? GameMode.HUMAN_VS_AI : GameMode.HUMAN_VS_HUMAN;
        AIDifficulty difficulty = AIDifficulty.EASY;

        if (mode == GameMode.HUMAN_VS_AI) {
            Object[] diffOptions = {"Easy", "Medium", "Hard"};
            int diffChoice = JOptionPane.showOptionDialog(
                    null, "Select AI difficulty:", "Chess",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, diffOptions, diffOptions[0]);

            if (diffChoice == 1)      difficulty = AIDifficulty.MEDIUM;
            else if (diffChoice == 2) difficulty = AIDifficulty.HARD;
        }

        JFrame window = new JFrame("Java Chess");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gp = new GamePanel(mode, difficulty);
        window.add(gp);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gp.launchGame();
    }
}
