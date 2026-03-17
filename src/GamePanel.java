import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {

    // --- Constants ---
    static final int SCREEN_WIDTH  = 1300;
    static final int SCREEN_HEIGHT = 750;
    static final int UNIT_SIZE     = 50;
    static final int GAME_UNITS    = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
    static final int INITIAL_DELAY = 150;
    static final int MIN_DELAY     = 60;

    // --- Game state enum ---
    enum GameState { START, RUNNING, PAUSED, DEATH_FLASH, GAME_OVER }
    GameState state = GameState.START;

    // --- Snake ---
    final int[] x = new int[GAME_UNITS];
    final int[] y = new int[GAME_UNITS];
    int bodyParts = 6;
    char direction = 'R';

    // --- Food ---
    int appleX, appleY;
    boolean goldenApple  = false;
    int goldenTicksLeft  = 0;
    static final int GOLDEN_DURATION = 53; // ~8 s at 150 ms/tick

    // --- Collision flash ---
    int collisionIndex = -1;   // body index the head hit (-1 = wall)
    int deathFlashTicks = 0;
    static final int DEATH_FLASH_DURATION = 8; // ~1.2 s at 150 ms/tick

    // --- Score / speed ---
    int applesEaten = 0;
    int highScore   = 0;
    int currentDelay = INITIAL_DELAY;

    Timer timer;
    Random random;

    // --- High-score file ---
    static final String HS_FILE = "highscore.txt";

    // -------------------------------------------------------------------------
    GamePanel() {
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(new Color(15, 15, 20));
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        highScore = loadHighScore();
        // Start timer ticking so the start screen animates; game logic is gated by state
        timer = new Timer(INITIAL_DELAY, this);
        timer.start();
    }

    // -------------------------------------------------------------------------
    // Game lifecycle
    // -------------------------------------------------------------------------
    public void startGame() {
        applesEaten      = 0;
        bodyParts        = 6;
        direction        = 'R';
        goldenApple      = false;
        goldenTicksLeft  = 0;
        currentDelay     = INITIAL_DELAY;
        collisionIndex   = -1;
        deathFlashTicks  = 0;

        // Reset snake position to center-left, snapped to the grid
        int startX = (SCREEN_WIDTH  / 2 / UNIT_SIZE) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / 2 / UNIT_SIZE) * UNIT_SIZE;
        for (int i = 0; i < bodyParts; i++) {
            x[i] = startX - i * UNIT_SIZE;
            y[i] = startY;
        }

        newApple();
        state = GameState.RUNNING;
        timer.setDelay(currentDelay);
    }

    private void newApple() {
        // 1-in-5 chance of golden apple
        goldenApple = (random.nextInt(5) == 0);
        if (goldenApple) goldenTicksLeft = GOLDEN_DURATION;
        appleX = random.nextInt(SCREEN_WIDTH  / UNIT_SIZE) * UNIT_SIZE;
        appleY = random.nextInt(SCREEN_HEIGHT / UNIT_SIZE) * UNIT_SIZE;
    }

    private void endGame() {
        state = GameState.GAME_OVER;
        timer.stop();
        if (applesEaten > highScore) {
            highScore = applesEaten;
            saveHighScore(highScore);
        }
    }

    // -------------------------------------------------------------------------
    // Main loop
    // -------------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.RUNNING) {
            if (goldenApple) {
                goldenTicksLeft--;
                if (goldenTicksLeft <= 0) {
                    goldenApple = false;
                }
            }
            move();
            checkApple();
            checkCollisions();
        } else if (state == GameState.DEATH_FLASH) {
            deathFlashTicks--;
            if (deathFlashTicks <= 0) {
                endGame();
            }
        }
        repaint();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    private void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
        switch (direction) {
            case 'U': y[0] -= UNIT_SIZE; break;
            case 'D': y[0] += UNIT_SIZE; break;
            case 'L': x[0] -= UNIT_SIZE; break;
            case 'R': x[0] += UNIT_SIZE; break;
        }
    }

    private void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            int points = goldenApple ? 3 : 1;
            applesEaten += points;
            bodyParts   += points;

            // Speed up every 5 apples
            int newDelay = INITIAL_DELAY - (applesEaten / 5) * 5;
            currentDelay = Math.max(MIN_DELAY, newDelay);
            timer.setDelay(currentDelay);

            newApple();
        }
    }

    private void checkCollisions() {
        // Head hits body
        for (int i = bodyParts; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) {
                collisionIndex  = i;
                startDeathFlash();
                return;
            }
        }
        // Head hits walls
        if (x[0] < 0 || x[0] >= SCREEN_WIDTH || y[0] < 0 || y[0] >= SCREEN_HEIGHT) {
            collisionIndex = -1;
            startDeathFlash();
        }
    }

    private void startDeathFlash() {
        state = GameState.DEATH_FLASH;
        deathFlashTicks = DEATH_FLASH_DURATION;
        // Keep timer running so ticks count down; movement is gated by state
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (state) {
            case START:       drawStartScreen(g2);              break;
            case RUNNING:     drawGame(g2);                     break;
            case PAUSED:      drawGame(g2); drawPauseOverlay(g2); break;
            case DEATH_FLASH: drawDeathFlash(g2);               break;
            case GAME_OVER:   drawGame(g2); drawGameOver(g2);   break;
        }
    }

    // --- Grid background ---
    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(30, 30, 40));
        for (int col = 0; col < SCREEN_WIDTH; col += UNIT_SIZE) {
            g.drawLine(col, 0, col, SCREEN_HEIGHT);
        }
        for (int row = 0; row < SCREEN_HEIGHT; row += UNIT_SIZE) {
            g.drawLine(0, row, SCREEN_WIDTH, row);
        }
    }

    // --- Apple ---
    private void drawApple(Graphics2D g) {
        Color baseColor  = goldenApple ? new Color(255, 200, 0) : new Color(220, 50, 50);
        Color glowColor  = goldenApple ? new Color(255, 200, 0, 60) : new Color(220, 50, 50, 60);

        // Glow halo
        int pad = 8;
        g.setColor(glowColor);
        g.fillOval(appleX - pad, appleY - pad, UNIT_SIZE + pad * 2, UNIT_SIZE + pad * 2);

        // Apple body
        g.setColor(baseColor);
        g.fillOval(appleX + 3, appleY + 3, UNIT_SIZE - 6, UNIT_SIZE - 6);

        // Stem
        g.setColor(new Color(100, 60, 20));
        g.setStroke(new BasicStroke(2));
        int cx = appleX + UNIT_SIZE / 2;
        g.drawLine(cx, appleY + 3, cx + 4, appleY - 6);

        // Leaf
        g.setColor(new Color(50, 160, 50));
        g.fillOval(cx, appleY - 8, 10, 6);

        g.setStroke(new BasicStroke(1));
    }

    // --- Snake ---
    private void drawSnake(Graphics2D g) {
        for (int i = bodyParts - 1; i >= 0; i--) {
            if (i == 0) {
                // Head
                g.setColor(new Color(0, 200, 80));
                g.fillRoundRect(x[i] + 1, y[i] + 1, UNIT_SIZE - 2, UNIT_SIZE - 2, 16, 16);
                drawEyes(g, x[i], y[i]);
            } else {
                // Body color milestones
                Color bodyColor;
                if (applesEaten >= 50) {
                    bodyColor = new Color(random.nextInt(200) + 55, random.nextInt(200) + 55, random.nextInt(200) + 55);
                } else if (applesEaten >= 25) {
                    bodyColor = new Color(148, 63, 236);
                } else if (applesEaten >= 10) {
                    bodyColor = new Color(230, 210, 0);
                } else {
                    bodyColor = new Color(45, 180, 0);
                }
                g.setColor(bodyColor);
                g.fillRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);

                // Subtle border
                g.setColor(bodyColor.darker());
                g.drawRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);
            }
        }
    }

    private void drawEyes(Graphics2D g, int hx, int hy) {
        int eyeSize = 8;
        int pupilSize = 4;
        int ex1, ey1, ex2, ey2;

        switch (direction) {
            case 'R':
                ex1 = hx + UNIT_SIZE - 14; ey1 = hy + 10;
                ex2 = hx + UNIT_SIZE - 14; ey2 = hy + UNIT_SIZE - 18;
                break;
            case 'L':
                ex1 = hx + 6;  ey1 = hy + 10;
                ex2 = hx + 6;  ey2 = hy + UNIT_SIZE - 18;
                break;
            case 'U':
                ex1 = hx + 10;           ey1 = hy + 6;
                ex2 = hx + UNIT_SIZE - 18; ey2 = hy + 6;
                break;
            default: // 'D'
                ex1 = hx + 10;           ey1 = hy + UNIT_SIZE - 14;
                ex2 = hx + UNIT_SIZE - 18; ey2 = hy + UNIT_SIZE - 14;
                break;
        }

        g.setColor(Color.WHITE);
        g.fillOval(ex1, ey1, eyeSize, eyeSize);
        g.fillOval(ex2, ey2, eyeSize, eyeSize);

        g.setColor(Color.BLACK);
        g.fillOval(ex1 + 2, ey1 + 2, pupilSize, pupilSize);
        g.fillOval(ex2 + 2, ey2 + 2, pupilSize, pupilSize);
    }

    // --- Score HUD ---
    private void drawHUD(Graphics2D g) {
        String scoreText = "Score: " + applesEaten;
        String hsText    = "Best: " + highScore;
        String speedText = "Speed: " + Math.round((INITIAL_DELAY / (double) currentDelay) * 100) / 100.0 + "x";

        g.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();

        // Score — top center
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect((SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2 - 10, 8,
                fm.stringWidth(scoreText) + 20, 34, 10, 10);
        g.setColor(new Color(255, 80, 80));
        g.drawString(scoreText, (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2, 32);

        // Best — top left
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(10, 8, fm.stringWidth(hsText) + 20, 34, 10, 10);
        g.setColor(new Color(255, 200, 0));
        g.drawString(hsText, 20, 32);

        // Speed — top right
        g.setColor(new Color(0, 0, 0, 120));
        int sw = fm.stringWidth(speedText);
        g.fillRoundRect(SCREEN_WIDTH - sw - 30, 8, sw + 20, 34, 10, 10);
        g.setColor(new Color(100, 200, 255));
        g.drawString(speedText, SCREEN_WIDTH - sw - 20, 32);

        // Golden apple timer bar
        if (goldenApple) {
            int barW = 200;
            int barH = 12;
            int barX = (SCREEN_WIDTH - barW) / 2;
            int barY = SCREEN_HEIGHT - 30;
            int filled = (int) ((goldenTicksLeft / (double) GOLDEN_DURATION) * barW);

            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 8, 8);
            g.setColor(new Color(255, 200, 0, 180));
            g.fillRoundRect(barX, barY, filled, barH, 6, 6);
            g.setColor(new Color(255, 200, 0));
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString("Golden Apple!", (SCREEN_WIDTH - g.getFontMetrics().stringWidth("Golden Apple!")) / 2, barY - 4);
        }
    }

    // --- Full game view ---
    private void drawGame(Graphics2D g) {
        drawGrid(g);
        drawApple(g);
        drawSnake(g);
        drawHUD(g);
    }

    // --- Death flash: frozen frame with head + colliding segment highlighted ---
    private void drawDeathFlash(Graphics2D g) {
        drawGrid(g);
        drawApple(g);

        // Pulsing alpha so the highlights strobe (odd ticks bright, even ticks dim)
        boolean bright = (deathFlashTicks % 2 == 0);

        for (int i = bodyParts - 1; i >= 0; i--) {
            if (i == 0) {
                // Head — red with glow
                Color glow = new Color(255, 30, 30, bright ? 80 : 30);
                int pad = 6;
                g.setColor(glow);
                g.fillRoundRect(x[i] - pad, y[i] - pad, UNIT_SIZE + pad * 2, UNIT_SIZE + pad * 2, 20, 20);

                g.setColor(new Color(255, 60, 60));
                g.fillRoundRect(x[i] + 1, y[i] + 1, UNIT_SIZE - 2, UNIT_SIZE - 2, 16, 16);
                drawEyes(g, x[i], y[i]);

            } else if (i == collisionIndex) {
                // The segment the head collided with — orange with glow
                Color glow = new Color(255, 140, 0, bright ? 100 : 40);
                int pad = 5;
                g.setColor(glow);
                g.fillRoundRect(x[i] - pad, y[i] - pad, UNIT_SIZE + pad * 2, UNIT_SIZE + pad * 2, 18, 18);

                g.setColor(new Color(255, 150, 0));
                g.fillRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);
                g.setColor(new Color(200, 100, 0));
                g.drawRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);

            } else {
                // Normal body — slightly dimmed
                Color bodyColor;
                if (applesEaten >= 50) {
                    bodyColor = new Color(100, 100, 100);
                } else if (applesEaten >= 25) {
                    bodyColor = new Color(100, 45, 160);
                } else if (applesEaten >= 10) {
                    bodyColor = new Color(160, 145, 0);
                } else {
                    bodyColor = new Color(30, 120, 0);
                }
                g.setColor(bodyColor);
                g.fillRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);
                g.setColor(bodyColor.darker());
                g.drawRoundRect(x[i] + 2, y[i] + 2, UNIT_SIZE - 4, UNIT_SIZE - 4, 12, 12);
            }
        }

        drawHUD(g);
    }

    // --- Start screen ---
    private void drawStartScreen(Graphics2D g) {
        // Subtle grid
        drawGrid(g);

        // Title glow
        g.setFont(new Font("Segoe UI", Font.BOLD, 100));
        FontMetrics fm = g.getFontMetrics();
        String title = "SNAKE";
        int tx = (SCREEN_WIDTH - fm.stringWidth(title)) / 2;
        int ty = SCREEN_HEIGHT / 2 - 60;

        g.setColor(new Color(0, 200, 80, 40));
        g.drawString(title, tx - 4, ty + 4);
        g.setColor(new Color(0, 200, 80, 80));
        g.drawString(title, tx - 2, ty + 2);
        g.setColor(new Color(0, 230, 90));
        g.drawString(title, tx, ty);

        // Subtitle
        g.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        fm = g.getFontMetrics();
        String sub = "Press  ENTER  to Play";
        g.setColor(new Color(180, 180, 180));
        g.drawString(sub, (SCREEN_WIDTH - fm.stringWidth(sub)) / 2, ty + 70);

        // Controls hint
        g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        String ctrl = "Arrow Keys to move   |   SPACE to pause";
        g.setColor(new Color(100, 100, 100));
        g.drawString(ctrl, (SCREEN_WIDTH - fm.stringWidth(ctrl)) / 2, ty + 110);

        // High score
        if (highScore > 0) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            fm = g.getFontMetrics();
            String hs = "Best Score: " + highScore;
            g.setColor(new Color(255, 200, 0));
            g.drawString(hs, (SCREEN_WIDTH - fm.stringWidth(hs)) / 2, ty + 160);
        }
    }

    // --- Pause overlay ---
    private void drawPauseOverlay(Graphics2D g) {
        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(new Color(10, 10, 20));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setComposite(orig);

        g.setFont(new Font("Segoe UI", Font.BOLD, 80));
        FontMetrics fm = g.getFontMetrics();
        String txt = "PAUSED";
        int tx = (SCREEN_WIDTH - fm.stringWidth(txt)) / 2;
        int ty = SCREEN_HEIGHT / 2;

        g.setColor(new Color(255, 255, 255, 40));
        g.drawString(txt, tx - 2, ty + 2);
        g.setColor(Color.WHITE);
        g.drawString(txt, tx, ty);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        fm = g.getFontMetrics();
        String hint = "Press  SPACE  to resume";
        g.setColor(new Color(160, 160, 160));
        g.drawString(hint, (SCREEN_WIDTH - fm.stringWidth(hint)) / 2, ty + 50);
    }

    // --- Game over overlay ---
    private void drawGameOver(Graphics2D g) {
        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.70f));
        g.setColor(new Color(10, 10, 20));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setComposite(orig);

        int cy = SCREEN_HEIGHT / 2 - 60;

        // "Game Over"
        g.setFont(new Font("Segoe UI", Font.BOLD, 80));
        FontMetrics fm = g.getFontMetrics();
        String over = "Game Over";
        g.setColor(new Color(220, 50, 50, 60));
        g.drawString(over, (SCREEN_WIDTH - fm.stringWidth(over)) / 2 - 3, cy + 3);
        g.setColor(new Color(220, 50, 50));
        g.drawString(over, (SCREEN_WIDTH - fm.stringWidth(over)) / 2, cy);

        // Score
        g.setFont(new Font("Segoe UI", Font.BOLD, 36));
        fm = g.getFontMetrics();
        String scoreStr = "Score: " + applesEaten;
        g.setColor(new Color(255, 80, 80));
        g.drawString(scoreStr, (SCREEN_WIDTH - fm.stringWidth(scoreStr)) / 2, cy + 65);

        // High score
        String hsStr = (applesEaten >= highScore && applesEaten > 0)
                ? "New Best: " + highScore + "!"
                : "Best: " + highScore;
        g.setColor(new Color(255, 200, 0));
        g.drawString(hsStr, (SCREEN_WIDTH - fm.stringWidth(hsStr)) / 2, cy + 110);

        // Restart prompt
        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        fm = g.getFontMetrics();
        String restart = "Press  ENTER  to Play Again";
        g.setColor(new Color(160, 160, 160));
        g.drawString(restart, (SCREEN_WIDTH - fm.stringWidth(restart)) / 2, cy + 160);
    }

    // -------------------------------------------------------------------------
    // High score persistence
    // -------------------------------------------------------------------------
    private int loadHighScore() {
        try (BufferedReader br = new BufferedReader(new FileReader(HS_FILE))) {
            String line = br.readLine();
            if (line != null) return Integer.parseInt(line.trim());
        } catch (Exception ignored) {}
        return 0;
    }

    private void saveHighScore(int score) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(HS_FILE))) {
            bw.write(String.valueOf(score));
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------
    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (state == GameState.RUNNING && direction != 'R') direction = 'L';
                    break;
                case KeyEvent.VK_RIGHT:
                    if (state == GameState.RUNNING && direction != 'L') direction = 'R';
                    break;
                case KeyEvent.VK_UP:
                    if (state == GameState.RUNNING && direction != 'D') direction = 'U';
                    break;
                case KeyEvent.VK_DOWN:
                    if (state == GameState.RUNNING && direction != 'U') direction = 'D';
                    break;
                case KeyEvent.VK_SPACE:
                    if (state == GameState.RUNNING) {
                        state = GameState.PAUSED;
                        timer.stop();
                    } else if (state == GameState.PAUSED) {
                        state = GameState.RUNNING;
                        timer.start();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    if (state == GameState.START || state == GameState.GAME_OVER) {
                        startGame();
                        timer.start();
                    }
                    break;
            }
        }
    }
}
