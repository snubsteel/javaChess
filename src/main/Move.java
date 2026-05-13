package main;

import piece.Piece;

public class Move {

    public final Piece piece;
    public final int toCol;
    public final int toRow;
    // null for normal moves; set to QUEEN/ROOK/BISHOP/KNIGHT to auto-promote
    public final Type promoteTo;

    public Move(Piece piece, int toCol, int toRow) {
        this.piece = piece;
        this.toCol = toCol;
        this.toRow = toRow;
        this.promoteTo = null;
    }

    public Move(Piece piece, int toCol, int toRow, Type promoteTo) {
        this.piece = piece;
        this.toCol = toCol;
        this.toRow = toRow;
        this.promoteTo = promoteTo;
    }
}
