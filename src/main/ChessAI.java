package main;

import java.util.ArrayList;
import java.util.Random;

public class ChessAI {

    private static final Random random = new Random();
    private static final int DEPTH_HARD = 2;
    private static final int CHECKMATE_SCORE = 1_000_000;

    /**
     * Returns a move for the AI side, or null if no legal moves exist.
     */
    public Move getMove(GamePanel gp, AIDifficulty difficulty, int color) {
        switch (difficulty) {
            case MEDIUM:
                return getMediumMove(gp, color);
            case HARD:
                return getHardMove(gp, color);
            default:
                return getEasyMove(gp, color);
        }
    }

    // -----------------------------------------------------------------
    // Easy: random legal move
    // -----------------------------------------------------------------
    private Move getEasyMove(GamePanel gp, int color) {
        ArrayList<Move> moves = gp.getLegalMoves(color);
        if (moves.isEmpty()) {
            return null;
        }
        return moves.get(random.nextInt(moves.size()));
    }

    // -----------------------------------------------------------------
    // Medium: 1-ply greedy with positional scoring; random tie-break
    // -----------------------------------------------------------------
    private Move getMediumMove(GamePanel gp, int color) {
        ArrayList<Move> moves = gp.getLegalMoves(color);
        if (moves.isEmpty()) {
            return null;
        }

        ArrayList<Move> best = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            Object snap = gp.takeSnapshot();
            if (gp.applyMove(move)) {
                int score = gp.evaluateBoard(color);
                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                    best.add(move);
                } else if (score == bestScore) {
                    best.add(move);
                }
            }
            gp.restoreSnapshot(snap);
        }

        if (best.isEmpty()) {
            return moves.get(0);
        }
        return best.get(random.nextInt(best.size()));
    }

    // -----------------------------------------------------------------
    // Hard: minimax with alpha-beta pruning
    // -----------------------------------------------------------------
    private Move getHardMove(GamePanel gp, int color) {
        ArrayList<Move> moves = gp.getLegalMoves(color);
        if (moves.isEmpty()) {
            return null;
        }

        ArrayList<Move> best = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE + 1;
        int alpha = Integer.MIN_VALUE + 1;
        int beta = Integer.MAX_VALUE;
        int opponent = opponent(color);

        long startMs = System.currentTimeMillis();

        for (Move move : moves) {
            Object snap = gp.takeSnapshot();
            if (gp.applyMove(move)) {
                int score = minimax(gp, DEPTH_HARD - 1, alpha, beta, false, color, opponent);
                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                    best.add(move);
                } else if (score == bestScore) {
                    best.add(move);
                }
                alpha = Math.max(alpha, bestScore);
            }
            gp.restoreSnapshot(snap);
        }

        System.out.println("Hard AI selected move in " + (System.currentTimeMillis() - startMs) + " ms");

        if (best.isEmpty()) {
            return moves.get(0);
        }
        return best.get(random.nextInt(best.size()));
    }

    /**
     * Minimax with alpha-beta pruning. The score is always from
     * {@code aiColor}'s perspective (higher = better for AI).
     *
     * @param isMaximizing true when it is the AI's (aiColor's) turn to move
     * @param aiColor the root AI player's color
     * @param currentColor the color to move at this node
     */
    private int minimax(GamePanel gp, int depth, int alpha, int beta,
            boolean isMaximizing, int aiColor, int currentColor) {
        // Terminal checks first (set by the parent's applyMove)
        if (gp.gameover) {
            // The player whose turn it now is was just checkmated
            return isMaximizing ? -CHECKMATE_SCORE : CHECKMATE_SCORE;
        }
        if (gp.stalemate) {
            return 0;
        }
        if (depth == 0) {
            return gp.evaluateBoard(aiColor);
        }

        ArrayList<Move> moves = gp.getLegalMoves(currentColor);
        if (moves.isEmpty()) {
            // Stalemate not caught by the flag (incomplete existing detection)
            return 0;
        }

        int opponent = opponent(currentColor);

        if (isMaximizing) {
            int best = Integer.MIN_VALUE + 1;
            for (Move move : moves) {
                Object snap = gp.takeSnapshot();
                if (gp.applyMove(move)) {
                    int score = minimax(gp, depth - 1, alpha, beta, false, aiColor, opponent);
                    best = Math.max(best, score);
                    alpha = Math.max(alpha, best);
                }
                gp.restoreSnapshot(snap);
                if (beta <= alpha) {
                    break; // beta cut-off
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (Move move : moves) {
                Object snap = gp.takeSnapshot();
                if (gp.applyMove(move)) {
                    int score = minimax(gp, depth - 1, alpha, beta, true, aiColor, opponent);
                    best = Math.min(best, score);
                    beta = Math.min(beta, best);
                }
                gp.restoreSnapshot(snap);
                if (beta <= alpha) {
                    break; // alpha cut-off
                }
            }
            return best;
        }
    }

    private static int opponent(int color) {
        return color == GamePanel.WHITE ? GamePanel.BLACK : GamePanel.WHITE;
    }
}
