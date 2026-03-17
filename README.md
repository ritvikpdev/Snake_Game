# Snake Game

A modern Snake game built with Java Swing, featuring smooth visuals, progressive difficulty, golden apples, a persistent high score, and polished UI screens.

## Download

**[Download for Windows (no Java required)](../../releases/latest)**

1. Download `SnakeGame-windows.zip` from the link above
2. Extract the zip
3. Double-click `SnakeGame.exe` — that's it
   OR
Download the jar file https://github.com/ritvikpdev/Snake_Game/blob/main/SnakeGame.jar and just run it.

---

## How to Play

| Key | Action |
|-----|--------|
| `Enter` | Start / Restart the game |
| `Arrow Keys` | Move the snake |
| `Space` | Pause / Resume |

---

## Features

- **Start screen** — title screen with your all-time best score displayed
- **Rounded snake** — pill-shaped body segments with directional eyes on the head
- **Apple with glow** — red apple with a halo, stem, and leaf
- **Golden Apple** — spawns randomly (1-in-5 chance), worth **3 points**, expires after 8 seconds
- **Progressive speed** — game speeds up every 5 apples eaten
- **Color milestones** — snake body changes color as your score grows:
  - Score 0–9: Green
  - Score 10–24: Yellow
  - Score 25–49: Purple
  - Score 50+: Rainbow
- **Death flash** — when the snake hits itself, the head and the collided segment flash red/orange before the game ends
- **Persistent high score** — your best score is saved to `highscore.txt` and survives between sessions
- **Pause overlay** — semi-transparent pause screen
- **Game over screen** — shows final score, best score, and restart prompt

---

## Running the Game

### Option 1 — Run from source (requires Java 8+)

**Compile:**
```bash
javac -d bin src/GamePanel.java src/GameFrame.java src/SnakeGame.java
```

**Run:**
```bash
java -cp bin SnakeGame
```

---

### Option 2 — Run the JAR (requires Java 8+)

**Create the JAR:**
```bash
jar cfe SnakeGame.jar SnakeGame -C bin .
```

**Run:**
```bash
java -jar SnakeGame.jar
```

---

## Project Structure

```
Snake_Game/
├── src/
│   ├── SnakeGame.java      # Entry point
│   ├── GameFrame.java      # JFrame window
│   └── GamePanel.java      # All game logic and rendering
├── bin/                    # Compiled .class files
├── output/
│   └── SnakeGame/
│       └── SnakeGame.exe   # Standalone Windows executable
├── highscore.txt           # Auto-generated; stores your best score
├── SnakeGame.jar           # Runnable JAR (if built)
└── README.md
```

---

## Requirements (source / JAR only)

- Java 8 or higher to run from source or JAR
- Java 14 or higher to rebuild the `.exe` via `jpackage`
- No external libraries — pure Java Swing
