# Java Chess

A Java Swing chess game with local two-player mode and an optional AI opponent.

![image](https://github.com/user-attachments/assets/a02bb1eb-75b5-45c7-900e-8ea89a035e12)

## Features

- Human vs human play
- Human vs AI play
- Easy, Medium, and Hard AI difficulty options
- Legal move validation
- Check, checkmate, and basic stalemate handling
- Special move support for castling, en passant, and pawn promotion

Note: Medium and Hard currently use the same random-move behavior as Easy. They are wired into the UI so stronger AI can be added incrementally.

## Requirements

- Java JDK 8 or newer
- PowerShell, Command Prompt, or another terminal

## Build

From the project root:

```powershell
New-Item -ItemType Directory -Force out
javac -cp "src;res" -d out src\main\*.java src\piece\*.java
```

This compiles generated `.class` files into `out/` instead of mixing them with source files.

## Run

```powershell
java -cp "out;res" main.Main
```

When the game starts, choose either `Human vs Human` or `Human vs AI`. If AI mode is selected, choose a difficulty.

## Package

To build a runnable JAR:

```powershell
New-Item -ItemType Directory -Force dist
jar cfe dist\java-chess.jar main.Main -C out . -C res .
```

Run the JAR with:

```powershell
java -jar dist\java-chess.jar
```

## Project Structure

```text
src/main/    Application entry point, UI loop, board rendering, move application, AI wiring
src/piece/   Chess piece classes and movement rules
res/         Piece image assets
docs/        Architecture notes
```

## Development Notes

The current architecture keeps most game state and rule orchestration in `GamePanel`. Piece classes validate their own movement patterns against the shared simulated board state. AI support builds on two programmatic APIs:

- `getLegalMoves(int color)`
- `applyMove(Move move)`

This keeps the first AI implementation small while preserving the existing mouse-driven game behavior.
