package main;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import javax.swing.JPanel;
import piece.*;

public class GamePanel extends JPanel implements Runnable {

    public static final int GAME_WIDTH = 550;
    public static final int GAME_HEIGHT = 400;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP, checkingP;
    public static Piece castlingP;

    // COLOR
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // BOOLEANS
    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameover;
    boolean stalemate;

    // GAME MODE / AI
    GameMode gameMode;
    AIDifficulty aiDifficulty;
    ChessAI ai;
    int aiColor = BLACK;

    public GamePanel(GameMode mode, AIDifficulty difficulty) {
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        gameMode = mode;
        aiDifficulty = difficulty;
        ai = new ChessAI();

        setPieces();
        // testIllegal();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {

        // WHITE TEAM
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        // BLACK TEAM
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }

    public void testIllegal() {
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new King(WHITE, 3, 7));
        pieces.add(new King(BLACK, 0, 3));
        pieces.add(new Bishop(BLACK, 1, 4));
        pieces.add(new Queen(BLACK, 4, 5));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {

        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        // Game Loop
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {

            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

        }
    }

    private void update() {

        if (promotion) {
            promoting();

        } else if (gameover == false && stalemate == false) {

            if (gameMode == GameMode.HUMAN_VS_AI && currentColor == aiColor) {
                // AI TURN
                doAITurn();

            } else {
                // HUMAN TURN

                // MOUSE BUTTON PRESSED
                if (mouse.pressed) {
                    if (activeP == null) {
                        // If the activeP is null, check if you can pick up a piece.
                        for (Piece piece : simPieces) {
                            // If the mouse in on an ally piece, pick it up as the activeP.
                            if (piece.color == currentColor && piece.col == mouse.x / Board.SQUARE_SIZE
                                    && piece.row == mouse.y / Board.SQUARE_SIZE) {

                                activeP = piece;
                            }
                        }
                    } else {
                        // If the player is holding a piece, simulate the move.
                        simulate();
                    }
                }

                // MOUSE BUTTON RELEASED
                if (mouse.pressed == false) {

                    if (activeP != null) {

                        if (validSquare) {

                            // MOVE CONFIRMED
                            // Update the piece list in case a piece has been captured and removed during
                            // the simulation
                            copyPieces(simPieces, pieces);
                            activeP.updatePosition();
                            if (castlingP != null) {
                                castlingP.updatePosition();
                            }

                            if (isKingInCheck() && isCheckMate()) {
                                System.out.println("King is in check");
                                gameover = true;
                            } else if (isStaleMate() && isKingInCheck() == false) {
                                stalemate = true;
                            } else { // The game is still going on
                                if (canPromote()) {
                                    promotion = true;
                                } else {
                                    changePlayer();
                                }
                            }

                        } else {
                            // The move is not valid so reset everything
                            copyPieces(pieces, simPieces);
                            activeP.resetPosition();
                            activeP = null;
                        }

                    }
                }

            }

        }

    }

    private void doAITurn() {
        Move move = ai.getMove(this, aiDifficulty, aiColor);
        if (move != null) {
            applyMove(move);
        }
    }

    private void simulate() {

        canMove = false;
        validSquare = false;

        // Reset the piece list in every loop
        // This is basically for restoring the removed piece during the simulation
        copyPieces(pieces, simPieces);

        // Reset the castling piece's position
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // If a piece is being held, update its position.
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        // Check if the piece is hovering over a reachable square
        if (activeP.canMove(activeP.col, activeP.row)) {

            canMove = true;

            // If hitting a piece, remove it from the list
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }

            checkCastling();

            if (isIllegal(activeP) == false && opponentCanCaptureKing() == false) {
                validSquare = true;
            }
        }
    }

    private boolean isIllegal(Piece king) {

        if (king.type == Type.KING) {
            for (Piece piece : simPieces) {
                if (piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean opponentCanCaptureKing() {

        Piece king = getKing(false);

        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKingInCheck() {
        Piece king = getKing(true);
        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.canMove(king.col, king.row)) {
                checkingP = piece;
                return true;
            }
        }
        checkingP = null;
        return false;
    }

    private Piece getKing(boolean opponent) {
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING
                    && piece.color == (opponent ? (currentColor == WHITE ? BLACK : WHITE) : currentColor)) {
                return piece;
            }
        }
        return null;
    }

    private boolean isCheckMate() {

        Piece king = getKing(true);

        if (kingCanMove(king)) {
            return false;
        } else {
            // Check if you can block with another piece to avoid checkmate

            // Check the position of the checking piece and the king in check
            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);

            if (colDiff == 0) {
                // The checking piece is attacking vertically
                if (checkingP.row < king.row) {
                    // The checking piece is above the king
                    for (int row = checkingP.row; row < king.row; row++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
                if (checkingP.row > king.row) {
                    // The checking piece is below the king
                    for (int row = checkingP.row; row > king.row; row--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (rowDiff == 0) {
                // The checking piece is attacking horizontally
                if (checkingP.col < king.col) {
                    // The checking piece is to the left
                    for (int col = checkingP.col; col < king.col; col++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
                if (checkingP.col > king.col) {
                    // The checking piece is to the right
                    for (int col = checkingP.col; col > king.col; col--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (colDiff == rowDiff) {
                // The checking piece is attacking diagonally
                if (checkingP.row < king.row) {
                    // The checking piece is above the king
                    if (checkingP.col < king.col) {
                        // The checking piece is in the upper left
                        for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if (checkingP.col > king.col) {
                        // The checking piece is in the upper right
                        for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                if (checkingP.row > king.row) {
                    // The checking piece is below the king
                    if (checkingP.col < king.col) {
                        // The checking piece is in the lower left
                        for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if (checkingP.col > king.col) {
                        // The checking piece is in the lower right
                        for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean kingCanMove(Piece king) {

        // Simulate if there is any square where the king can move to
        if (isValidMove(king, -1, -1)) {
            return true;
        }
        if (isValidMove(king, 0, -1)) {
            return true;
        }
        if (isValidMove(king, 1, -1)) {
            return true;
        }
        if (isValidMove(king, -1, 0)) {
            return true;
        }
        if (isValidMove(king, 1, 0)) {
            return true;
        }
        if (isValidMove(king, -1, 1)) {
            return true;
        }
        if (isValidMove(king, 0, 1)) {
            return true;
        }
        if (isValidMove(king, 1, 1)) {
            return true;
        }

        return false;
    }

    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {

        boolean isValidMove = false;

        // Update the king's position for a second
        king.col += colPlus;
        king.row += rowPlus;

        if (king.canMove(king.col, king.row)) {

            if (king.hittingP != null) {
                simPieces.remove(king.hittingP.getIndex());
            }
            if (isIllegal(king) == false) {
                isValidMove = true;
            }
        }
        // Reset the king's position and restore the removed piece
        king.resetPosition();
        copyPieces(pieces, simPieces);

        return isValidMove;
    }

    private boolean isStaleMate() {

        int count = 0;
        // Count the number of pieces
        for (Piece piece : simPieces) {
            if (piece.color != currentColor) {
                count++;
            }
        }

        // If only one piece (the King) is left
        if (count == 1) {
            if (kingCanMove(getKing(true)) == false) {
                return true;
            }
        }
        return false;
    }

    private void checkCastling() {

        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {

        if (currentColor == WHITE) {
            currentColor = BLACK;
            // Reset black's two stepped status
            for (Piece piece : pieces) {
                if (piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        } else {
            currentColor = WHITE;
            // Reset white's two stepped status
            for (Piece piece : pieces) {
                if (piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activeP = null;
    }

    private boolean canPromote() {

        if (activeP.type == Type.PAWN) {
            if (currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }

        return false;
    }

    private void promoting() {

        if (mouse.pressed) {
            for (Piece piece : promoPieces) {
                if (piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                    switch (piece.type) {
                        case ROOK:
                            simPieces.add(new Rook(currentColor, activeP.col, activeP.row));
                            break;
                        case KNIGHT:
                            simPieces.add(new Knight(currentColor, activeP.col, activeP.row));
                            break;
                        case BISHOP:
                            simPieces.add(new Bishop(currentColor, activeP.col, activeP.row));
                            break;
                        case QUEEN:
                            simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
                            break;
                        default:
                            break;
                    }
                    simPieces.remove(activeP.getIndex());
                    copyPieces(simPieces, pieces);
                    activeP = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Legal-move generation
    // -----------------------------------------------------------------
    /**
     * Captures every mutable field of one Piece so it can be fully restored.
     */
    private static class PieceState {

        final Piece piece;
        final int col, row, x, y, preCol, preRow;
        final boolean moved, twoStepped;
        final Piece hittingP;

        PieceState(Piece p) {
            piece = p;
            col = p.col;
            row = p.row;
            x = p.x;
            y = p.y;
            preCol = p.preCol;
            preRow = p.preRow;
            moved = p.moved;
            twoStepped = p.twoStepped;
            hittingP = p.hittingP;
        }

        void restore() {
            piece.col = col;
            piece.row = row;
            piece.x = x;
            piece.y = y;
            piece.preCol = preCol;
            piece.preRow = preRow;
            piece.moved = moved;
            piece.twoStepped = twoStepped;
            piece.hittingP = hittingP;
        }
    }

    /**
     * Full snapshot of GamePanel state used by getLegalMoves to probe candidate
     * moves without permanently mutating anything.
     */
    private class GameStateSnapshot {

        final ArrayList<Piece> savedPieces;
        final ArrayList<Piece> savedSimPieces;
        final ArrayList<PieceState> pieceStates;
        final Piece savedActiveP, savedCastlingP, savedCheckingP;
        final int savedCurrentColor;
        final boolean savedCanMove, savedValidSquare;
        final boolean savedPromotion, savedGameover, savedStalemate;

        GameStateSnapshot() {
            savedPieces = new ArrayList<>(pieces);
            savedSimPieces = new ArrayList<>(simPieces);
            pieceStates = new ArrayList<>(pieces.size());
            for (Piece p : pieces) {
                pieceStates.add(new PieceState(p));
            }
            savedActiveP = activeP;
            savedCastlingP = castlingP;
            savedCheckingP = checkingP;
            savedCurrentColor = currentColor;
            savedCanMove = canMove;
            savedValidSquare = validSquare;
            savedPromotion = promotion;
            savedGameover = gameover;
            savedStalemate = stalemate;
        }

        void restore() {
            for (PieceState ps : pieceStates) {
                ps.restore();
            }
            pieces.clear();
            pieces.addAll(savedPieces);
            simPieces.clear();
            simPieces.addAll(savedSimPieces);
            activeP = savedActiveP;
            castlingP = savedCastlingP;
            checkingP = savedCheckingP;
            currentColor = savedCurrentColor;
            canMove = savedCanMove;
            validSquare = savedValidSquare;
            promotion = savedPromotion;
            gameover = savedGameover;
            stalemate = savedStalemate;
        }
    }

    /**
     * Returns every legal move available to the given color from the current
     * board position. Pawn promotions are returned as queen promotions. Does
     * not mutate board state, game flags, or whose turn it is.
     */
    public ArrayList<Move> getLegalMoves(int color) {
        ArrayList<Move> legal = new ArrayList<>();
        GameStateSnapshot snap = new GameStateSnapshot();

        // Build candidate list before any probing mutates the board
        ArrayList<Piece> candidates = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.color == color) {
                candidates.add(p);
            }
        }

        for (Piece piece : candidates) {
            for (int targetCol = 0; targetCol < 8; targetCol++) {
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    snap.restore(); // start each probe from clean state
                    if (probeLegal(piece, targetCol, targetRow, color)) {
                        boolean isPromoRow = piece.type == Type.PAWN
                                && targetRow == (color == WHITE ? 0 : 7);
                        legal.add(isPromoRow
                                ? new Move(piece, targetCol, targetRow, Type.QUEEN)
                                : new Move(piece, targetCol, targetRow));
                    }
                }
            }
        }

        snap.restore(); // leave board exactly as it was before the call
        return legal;
    }

    /**
     * Tests whether moving piece to (toCol, toRow) is legal for the given
     * color, using the same canMove/isIllegal/opponentCanCaptureKing chain as
     * applyMove. Mutates state freely; caller must restore via
     * GameStateSnapshot.
     */
    private boolean probeLegal(Piece piece, int toCol, int toRow, int color) {
        activeP = piece;
        currentColor = color; // ensures opponentCanCaptureKing finds the right king

        copyPieces(pieces, simPieces);
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        activeP.hittingP = null;
        activeP.col = toCol;
        activeP.row = toRow;

        if (!activeP.canMove(toCol, toRow)) {
            return false;
        }
        if (activeP.hittingP != null) {
            simPieces.remove(activeP.hittingP.getIndex());
        }
        checkCastling();
        return !isIllegal(activeP) && !opponentCanCaptureKing();
    }

    /**
     * Applies a move programmatically without mouse interaction. Returns true
     * if the move was legal and the board state was updated. Preserves all
     * existing rules: captures, castling, en passant, self-check prevention,
     * promotion, check/checkmate/stalemate.
     *
     * For pawn promotion: supply move.promoteTo (QUEEN/ROOK/BISHOP/KNIGHT) to
     * resolve automatically. If move.promoteTo is null, the game enters the
     * normal promotion state and waits for mouse input.
     */
    public boolean applyMove(Move move) {
        // Guard: reject invalid or untimely calls
        if (move == null || move.piece == null) {
            return false;
        }
        if (gameover || stalemate || promotion) {
            return false;
        }
        if (move.piece.color != currentColor) {
            return false;
        }
        if (!pieces.contains(move.piece)) {
            return false;
        }

        activeP = move.piece;

        // Reset simulation to the current authoritative state, same as simulate() does
        copyPieces(pieces, simPieces);
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // Clear stale hit state before canMove inspects the board
        activeP.hittingP = null;

        // Position the piece at the target square (mirrors simulate()'s mouse-to-col mapping)
        activeP.col = move.toCol;
        activeP.row = move.toRow;

        if (activeP.canMove(move.toCol, move.toRow)) {
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            checkCastling();

            if (!isIllegal(activeP) && !opponentCanCaptureKing()) {
                // Commit the move, mirroring the mouse-release confirmation block
                copyPieces(simPieces, pieces);
                activeP.updatePosition();
                if (castlingP != null) {
                    castlingP.updatePosition();
                }

                if (isKingInCheck() && isCheckMate()) {
                    gameover = true;
                } else if (isStaleMate() && !isKingInCheck()) {
                    stalemate = true;
                } else if (canPromote()) {
                    if (move.promoteTo != null) {
                        applyPromotion(move.promoteTo);
                    } else {
                        promotion = true; // caller must handle via UI
                    }
                } else {
                    changePlayer();
                }

                canMove = false;
                validSquare = false;
                if (!promotion) {
                    activeP = null;
                }
                return true;
            }
        }

        // Move was illegal -- undo all mutations to shared piece objects
        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        activeP.resetPosition();
        copyPieces(pieces, simPieces);
        canMove = false;
        validSquare = false;
        activeP = null;
        return false;
    }

    private void applyPromotion(Type promoteTo) {
        switch (promoteTo) {
            case ROOK:
                simPieces.add(new Rook(currentColor, activeP.col, activeP.row));
                break;
            case KNIGHT:
                simPieces.add(new Knight(currentColor, activeP.col, activeP.row));
                break;
            case BISHOP:
                simPieces.add(new Bishop(currentColor, activeP.col, activeP.row));
                break;
            default:
                simPieces.add(new Queen(currentColor, activeP.col, activeP.row));
                break;
        }
        simPieces.remove(activeP.getIndex());
        copyPieces(simPieces, pieces);
        activeP = null;
        promotion = false;
        changePlayer();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // BOARD
        board.draw(g2);

        // PIECES
        for (Piece p : simPieces) {
            p.draw(g2);
        }

        if (activeP != null) {
            if (canMove) {
                if (isIllegal(activeP) || opponentCanCaptureKing()) {
                    g2.setColor(Color.red);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                } else {
                    g2.setColor(Color.white);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }

            // Draw the active piece in the end so it won't be hidden by the board or the
            // colored square
            activeP.draw(g2);
        }

        // SIDEBAR
        g2.setColor(new Color(112, 128, 144)); // Color
        g2.fillRoundRect(400, 0, 150, 400, 70, 70); // Location & dimensions & round edges

        // SIDEBAR LINE LEFT
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(400, 0, 400, 400);

        // SIDERBAR LINE RIGHT
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(10));
        g2.drawLine(550, 0, 550, 550);

        // STATUS MESSAGES
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antiqua", Font.BOLD, 20));
        g2.setColor(new Color(255, 253, 208));

        if (promotion) {
            g2.drawString("Promote to:", 420, 65);
            for (Piece piece : promoPieces) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, null);
            }
        }

        if (currentColor == WHITE) {
            g2.drawString("White's turn", 417, 35);
            if (checkingP != null && checkingP.color == BLACK) {
                g2.setColor(Color.red);
                g2.drawString("The King", 420, 65);
                g2.drawString("is in check!", 420, 85);
            }
        } else {
            g2.setColor(Color.BLACK);
            g2.drawString("Black's turn", 417, 35);
            if (checkingP != null && checkingP.color == WHITE) {
                g2.setColor(Color.red);
                g2.drawString("The King", 420, 65);
                g2.drawString("is in check!", 420, 85);
            }
        }
        if (gameover) {
            String s = "";
            if (currentColor == WHITE) {
                g2.setColor(Color.white);
                s = "White Wins";
            } else {
                g2.setColor(Color.black);
                s = "Black Wins";
            }
            g2.setFont(new Font("Arial", Font.BOLD, 50));
            g2.drawString(s, 75, 200);
        }
        if (stalemate) {
            String s = "";
            if (currentColor == WHITE) {
                s = "Stalemate";
            } else {
                s = "Stalemate";
            }
            g2.setFont(new Font("Arial", Font.BOLD, 50));
            g2.setColor(Color.lightGray);
            g2.drawString(s, 75, 200);
        }
    }
}
