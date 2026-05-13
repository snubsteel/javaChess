# Architecture — Java Chess

## Overview

A two-player desktop chess game built in Java using Swing. The codebase is split into two packages: `main` (application infrastructure and game logic) and `piece` (piece hierarchy). The entire application runs in a single JFrame driven by a fixed 60 FPS game loop.

---

## Class Inventory

### `main` package

| Class | Responsibility |
|---|---|
| `Main` | Entry point. Creates the `JFrame`, instantiates `GamePanel`, and starts the game. |
| `GamePanel` | Central hub. Owns the game loop, all game state, all input handling, move validation, and rendering. Extends `JPanel`, implements `Runnable`. |
| `Board` | Draws the 8×8 alternating-color board grid. Defines the shared `SQUARE_SIZE = 50` and `HALF_SQUARE_SIZE = 25` constants. |
| `Mouse` | Extends `MouseAdapter`. Tracks mouse pixel coordinates (`x`, `y`) and button state (`pressed`) in real time. |
| `Type` | Enum of six piece types: `PAWN`, `ROOK`, `KNIGHT`, `BISHOP`, `QUEEN`, `KING`. |

### `piece` package

| Class | Responsibility |
|---|---|
| `Piece` | Base class for all pieces. Holds position, state, and image. Provides shared utilities for path blocking, square validation, and rendering. `canMove()` returns `false` by default. |
| `Pawn` | Extends `Piece`. Implements 1-square advance, 2-square initial advance, diagonal capture, and en passant. |
| `Rook` | Extends `Piece`. Implements horizontal/vertical sliding movement blocked by intervening pieces. |
| `Knight` | Extends `Piece`. Implements L-shaped jumps (product of col and row offsets == 2). Not blocked by intervening pieces. |
| `Bishop` | Extends `Piece`. Implements diagonal sliding movement blocked by intervening pieces. |
| `Queen` | Extends `Piece`. Combines Rook and Bishop movement. |
| `King` | Extends `Piece`. Implements 1-square movement in any direction, plus kingside and queenside castling. |

---

## Key Relationships and Dependencies

```
Main
 └─ creates ──► GamePanel (JPanel + Runnable)
                 ├─ owns ──► Board
                 ├─ owns ──► Mouse (MouseListener + MouseMotionListener)
                 ├─ owns ──► List<Piece>  pieces      (authoritative board state)
                 ├─ owns ──► List<Piece>  simPieces   (simulation working copy)  [static]
                 ├─ owns ──► List<Piece>  promoPieces (promotion UI pieces)
                 ├─ ref  ──► Piece        activeP     (currently dragged piece)
                 ├─ ref  ──► Piece        checkingP   (piece delivering check)
                 └─ ref  ──► Piece        castlingP   (rook during castling)     [static]

Piece  (base)
 ├─ subclassed by ──► Pawn, Rook, Knight, Bishop, Queen, King
 └─ reads static ──► GamePanel.simPieces  (for path-blocking and hitting checks)
```

**Coupling note:** `pieces`, `simPieces`, and `castlingP` are declared `public static` on `GamePanel`. All `Piece` subclasses reference `GamePanel.simPieces` directly. This is the primary coupling between the two packages.

---

## Data Flow: User Input → Board State

### 1. Input capture (continuous)
`Mouse` updates `x`, `y`, and `pressed` on every mouse event. No processing occurs here — it is pure state.

### 2. Game loop tick (60 FPS)
`GamePanel.run()` uses nanosecond delta timing. Each tick calls `update()` then `repaint()`.

### 3. Piece selection (`update()`, mouse just pressed)
When `mouse.pressed == true` and no piece is held (`activeP == null`), `simPieces` is scanned for a piece matching `currentColor` at the grid cell under the cursor. The first match becomes `activeP`.

### 4. Drag simulation (`simulate()`, called every frame while holding)
Each frame:
1. `simPieces` is **reset from `pieces`** — this undoes any hypothetical captures from the previous frame.
2. The castling rook (`castlingP`) is reset to its pre-move column.
3. `activeP`'s pixel position and grid cell are updated to follow the cursor.
4. `activeP.canMove(targetCol, targetRow)` is called on the piece's subclass implementation.
   - If `true`: `canMove = true`, `hittingP` (if any) is removed from `simPieces`, castling is checked.
   - Legality is then validated:
     - `isIllegal(activeP)` — if the active piece is the King, ensures no opponent can reach its target square.
     - `opponentCanCaptureKing()` — after the simulated move, ensures the current player's own king is not left in check.
   - If both checks pass: `validSquare = true`.

### 5. Move confirmation (mouse released)
If `validSquare == true`:
- `copyPieces(simPieces, pieces)` — commits the simulation (captured piece is now gone from the authoritative list).
- `activeP.updatePosition()` — snaps the piece to its target grid cell and sets `moved = true`. Also sets `twoStepped = true` if a pawn advanced two squares.
- `castlingP.updatePosition()` — moves the rook if a castling move occurred.

If `validSquare == false`:
- `copyPieces(pieces, simPieces)` — discards the simulation.
- `activeP.resetPosition()` — snaps the piece back to `preCol/preRow`.

### 6. Post-move state machine
```
isKingInCheck() && isCheckMate()  →  gameover = true
isStaleMate() && !isKingInCheck() →  stalemate = true
canPromote()                      →  promotion = true  (pause for UI)
else                              →  changePlayer()
```

### 7. Rendering (`paintComponent()`)
Draw order (back to front):
1. Board grid (`Board.draw()`)
2. All `simPieces` (committed + in-flight simulation)
3. Destination square highlight (white if valid, red if illegal) when dragging
4. `activeP` drawn last so it floats above everything
5. Sidebar (status text, promotion picker, game-over/stalemate overlay)

---

## GUI Framework and Rendering Approach

- **Framework:** Java Swing (`javax.swing`). `JFrame` is the window; `GamePanel` extends `JPanel` as the rendering surface.
- **Rendering:** Fully custom via `paintComponent(Graphics g)` cast to `Graphics2D`. No layout manager is used — all drawing is absolute pixel coordinates.
- **Game loop:** A dedicated `Thread` running `GamePanel.run()`. Nanosecond delta timing ensures a stable 60 FPS regardless of frame duration. Swing's `repaint()` is called each tick to schedule a paint.
- **Piece images:** PNG files in `res/piece_images/` (e.g., `w-pawn.png`, `b-king.png`). Loaded at piece construction time via `ImageIO.read(getClass().getResourceAsStream(...))` as classpath resources. Stored as `BufferedImage` on each `Piece` instance.
- **Alpha compositing:** `AlphaComposite.SRC_OVER` at 70% opacity is used to render the destination square highlight, then reset to 100% for subsequent draws.

---

## Patterns

### What's used

| Pattern | Where |
|---|---|
| **Template Method (informal)** | `Piece.canMove()` returns `false`; each subclass overrides with specific movement rules while calling shared helpers (`isWithinBoard`, `isValidSquare`, `pieceIsOnStraightLine`, `pieceIsOnDiagonalLine`). |
| **Dual-buffer state (simulation pattern)** | `pieces` is the committed truth; `simPieces` is a working copy. Every drag frame the working copy is reset, modified hypothetically, and used for legality checks without touching the authoritative list. |

### What's absent

There is no formal MVC, Observer, or Command pattern. `GamePanel` conflates model (state), controller (input → move logic), and view (rendering) into a single class. This is typical of tutorial-style game architecture.

---

## Game State

| Field | Type | Meaning |
|---|---|---|
| `currentColor` | `int` (0=WHITE, 1=BLACK) | Whose turn it is |
| `pieces` | `ArrayList<Piece>` | Authoritative, committed board state |
| `simPieces` | `ArrayList<Piece>` (static) | Simulation working copy; reset every drag frame |
| `promoPieces` | `ArrayList<Piece>` | Temporary list of promotion option pieces rendered in sidebar |
| `activeP` | `Piece` | The piece currently being dragged; `null` when idle |
| `checkingP` | `Piece` | The opponent piece delivering check; used only for rendering |
| `castlingP` | `Piece` (static) | The rook participating in the current castling move; `null` normally |
| `canMove` | `boolean` | Whether the drag target is geometrically reachable |
| `validSquare` | `boolean` | Whether the move is also legally safe (no self-check) |
| `promotion` | `boolean` | Pauses the game loop for the promotion picker |
| `gameover` | `boolean` | Set when checkmate is detected |
| `stalemate` | `boolean` | Set when stalemate is detected |

**`Piece`-level state:**

| Field | Meaning |
|---|---|
| `col, row` | Current (or in-flight) grid position |
| `preCol, preRow` | Last committed grid position; used for move-from origin |
| `x, y` | Pixel position (snaps to grid except during drag) |
| `moved` | `true` after first `updatePosition()` call; gates pawn 2-step and castling |
| `twoStepped` | `true` for one half-turn after a pawn's 2-square advance; enables en passant |
| `hittingP` | Set during `canMove()`; the piece on the target square, if any |

---

## Special Move Implementations

### En passant
`Pawn.canMove()` checks whether a piece at `(targetCol, preRow)` — the adjacent column on the same row — has `twoStepped == true`. If so, `hittingP` is set to that piece and `true` is returned. On confirmation, `hittingP` is removed from `simPieces` normally. `changePlayer()` clears `twoStepped` for all pieces of the newly active color, enforcing the one-turn window.

### Castling
`King.canMove()` checks the 2-square lateral move conditions and, when met, sets `GamePanel.castlingP` to the relevant rook. `GamePanel.checkCastling()` then repositions that rook within `simPieces`. On confirmation, both `activeP.updatePosition()` and `castlingP.updatePosition()` are called.

### Pawn promotion
When `canPromote()` detects a pawn on the back rank, it populates `promoPieces` with virtual pieces positioned at column 9 (off-board, rendered in the sidebar). The game loop enters the `promoting()` branch, which waits for a click on one of those options, then swaps the pawn for the selected piece type in `simPieces` and calls `changePlayer()`.

---

## Checkmate and Stalemate Detection

### Check (`isKingInCheck`)
After a move is committed, iterates `simPieces` looking for any opponent piece whose `canMove()` reaches the current player's king. Sets `checkingP` for UI feedback.

### Checkmate (`isCheckMate`)
1. Calls `kingCanMove(king)` — tests all 8 adjacent squares via `isValidMove()` (temporarily moves the king, checks `isIllegal`, then restores).
2. If the king has no escape: scans squares along the attack ray (vertical, horizontal, or diagonal) between `checkingP` and the king, testing whether any friendly piece can interpose or capture the attacker.

### Stalemate (`isStaleMate`)
Simplified: only triggers when exactly **one** opponent piece remains (the king) and `kingCanMove()` returns `false`. Does not detect stalemate when the opponent has multiple pieces that are all pinned.

---

## Notable Implementation Details

1. **`Piece.getY()` parameter name bug** (`Piece.java:47`): The method signature is `getY(int col)` but the body returns `row * Board.SQUARE_SIZE`, ignoring the parameter. All call sites pass `row` as the argument, so this is a misleading name rather than a functional bug. `getX` is symmetrically correct.

2. **Static fields on `GamePanel`**: `pieces`, `simPieces`, and `castlingP` are `public static`. This lets `Piece` instances reach back into `GamePanel` without a reference, at the cost of making the design harder to test or extend (e.g., running two simultaneous games would be impossible).

3. **`getIndex()` linear scan**: `Piece.getIndex()` does a reference-equality linear scan of `simPieces` to return the list index. Used only for `simPieces.remove(index)`. Could be replaced with `simPieces.remove(piece)` for clarity.

4. **No animation**: Pieces teleport to grid cells on release. During drag, `activeP` is drawn at the raw mouse pixel position offset by half a square so the piece centers under the cursor.

5. **Board coordinate system**: Column 0 is left (queenside for white), row 0 is top (black's back rank). White starts at rows 6–7, black at rows 0–1. White pawns move with `moveValue = -1` (upward), black with `moveValue = 1` (downward).

6. **Window dimensions**: `GAME_WIDTH = 550`, `GAME_HEIGHT = 400`. The board occupies 400×400 px (8 × 50). The sidebar occupies the remaining 150 px on the right.

7. **`testIllegal()` method**: A commented-out method in `GamePanel` that sets up a specific test position. Retained in the source as a debugging aid.

---

## File and Package Structure

```
javaChess/
├── src/
│   ├── main/
│   │   ├── Main.java          # Entry point
│   │   ├── GamePanel.java     # Game loop, state, logic, rendering
│   │   ├── Board.java         # Board drawing + size constants
│   │   ├── Mouse.java         # Raw mouse input adapter
│   │   └── Type.java          # Piece type enum
│   └── piece/
│       ├── Piece.java         # Base class with shared logic
│       ├── Pawn.java
│       ├── Rook.java
│       ├── Knight.java
│       ├── Bishop.java
│       ├── Queen.java
│       └── King.java
├── res/
│   └── piece_images/          # 12 PNG files (w/b × 6 piece types)
└── docs/
    └── ARCHITECTURE.md        # This document
```
