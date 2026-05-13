package main;

import java.util.ArrayList;
import java.util.Random;

public class ChessAI {

    private static final Random random = new Random();

    /** Returns a move for the AI side, or null if no legal moves exist. */
    public Move getMove(GamePanel gp, AIDifficulty difficulty, int color) {
        switch (difficulty) {
            case MEDIUM: return getMediumMove(gp, color);
            case HARD:   return getHardMove(gp, color);
            default:     return getEasyMove(gp, color);
        }
    }

    private Move getEasyMove(GamePanel gp, int color) {
        ArrayList<Move> moves = gp.getLegalMoves(color);
        if (moves.isEmpty()) return null;
        return moves.get(random.nextInt(moves.size()));
    }

    private Move getMediumMove(GamePanel gp, int color) {
        // Placeholder: upgrade to evaluation-based selection in a future step
        return getEasyMove(gp, color);
    }

    private Move getHardMove(GamePanel gp, int color) {
        // Placeholder: upgrade to minimax with alpha-beta in a future step
        return getEasyMove(gp, color);
    }
}
