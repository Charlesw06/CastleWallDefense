package main;

import entity.Arrow;
import entity.Player;
import entity.Enemy;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel implements Runnable {
    private final int originalTileSize = 16;
    private final int scale = 9;
    private final int tileSize = originalTileSize * scale;
    private final int maxScreenCol = 9;
    private final int maxScreenRow = 5;
    private final int screenWidth = tileSize * maxScreenCol;
    private final int screenHeight = tileSize * maxScreenRow;
    private KeyHandler keyH = new KeyHandler();
    private BufferedImage background;
    private String backgroundState;

    private Thread gameThread;
    private int enemyCooldown;
    private int frameCount;
    private int enemySpeed;
    private Player p;
    private ArrayList<Arrow> arrows;
    private ArrayList<Enemy> enemies;
    private int fps;
    private int barricadeHealth;
    private int killCount;
    private int killsUntilSpeedBuff;
    private final int[] yValues = {0, 144, 288, 432, 576};


    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        setValues();
    }

    private void setValues() {
        arrows = new ArrayList<Arrow>();
        enemies = new ArrayList<Enemy>();
        fps = 60;
        enemyCooldown = 120;
        frameCount = 0;
        enemySpeed = 5;
        barricadeHealth = 100;
        killCount = 0;
        killsUntilSpeedBuff = 10;
        p = new Player(110, tileSize * 2, tileSize, this, keyH);

        backgroundState = "title screen";

        try {
            background = ImageIO.read(getClass().getResourceAsStream("/Background/Background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void update() {
        p.update();
        frameCount++;

        if(frameCount == enemyCooldown) {
            int x = maxScreenCol * tileSize;
            int i = (int) (Math.random() * yValues.length);
            Enemy e = new Enemy (x, yValues[i], enemySpeed);
            enemies.add(e);
            frameCount = 0;
        }

        if(keyH.isSpacePressed()) {
            boolean tooClose = false;

            for(Arrow a : arrows) {
                if(a.x < p.x + 300) {
                    tooClose = true;
                }
            }
            if(!tooClose) {
                Arrow a = new Arrow(p.x, p.y, 20);
                arrows.add(a);
                System.out.println(arrows);
            }
        }

        for(int i = 0; i < arrows.size(); i++) {
            Arrow a = arrows.get(i);
            if(a.x > tileSize * maxScreenCol) {
                arrows.remove(i);
            }
            else {
                a.moveForward();
            }

            for(int k = 0; k < enemies.size(); k++) {
                Enemy e = enemies.get(k);
                if(a.y == e.y) {
                    if(a.x >= e.x) {
                        killCount++;
                        killsUntilSpeedBuff--;
                        enemies.remove(k);
                        arrows.remove(i);
                        k = enemies.size();
                    }
                }
            }

            if(killsUntilSpeedBuff == 0) {
                enemySpeed += 2;
                if (enemyCooldown != 60) {
                    enemyCooldown -= 20;
                }
                for(Enemy e : enemies) {
                    e.setSpeed(enemySpeed);
                }
                killsUntilSpeedBuff = 10;
            }

        }

        for (int i = 0; i < enemies.size(); i++){
            Enemy e = enemies.get(i);
            if (e.x + e.speed > 200) {
                e.moveForward();
            }
            else if (e.getState().equals("attacking") && e.getFrameCount() <= 25) {
                if(barricadeHealth != 0) {
                    barricadeHealth -= 10;
                }
                enemies.remove(i);
                i--;
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g;

        if(backgroundState.equals("title screen")) {
            this.setBackground(Color.black);
            this.setForeground(Color.white);
            Font newFont = new Font("TimesRoman", Font.BOLD, 30);
            graphics2D.setFont(newFont);
            graphics2D.drawString("Welcome to Castle Wall Defense!", 400, 100);
            graphics2D.drawString("Press ENTER/RETURN to start", 400, 160);
        }
        if(backgroundState.equals("ended")) {

        }
        if(backgroundState.equals("playing")) {
            graphics2D.drawImage(background, 0, 0, tileSize * maxScreenCol, tileSize * maxScreenRow, null);
            p.draw(graphics2D, tileSize);
            for (Arrow a : arrows) {
                a.draw(graphics2D, tileSize);
            }
            for (Enemy e : enemies) {
                e.draw(graphics2D, tileSize);
            }

            graphics2D.setColor(Color.black);
            Font newFont = new Font("TimesRoman", Font.BOLD, 30);
            graphics2D.setFont(newFont);
            graphics2D.drawString("Kill Count: " + killCount, 30, 30);
            graphics2D.drawString("Barricade Health: " + barricadeHealth + "/100", 925, 30);
        }
        graphics2D.dispose();
    }


    @Override
    public void run() {
        double drawInterval = 1000000000/fps;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (gameThread != null) {
            if (backgroundState.equals("playing")) {
                update();
            }
            repaint();

            if (keyH.isEnterPressed() && backgroundState.equals("title screen")) {
                backgroundState = "playing";
            }

            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime = remainingTime/1000000;

                if (remainingTime < 0) {
                    remainingTime = 0;
                }

                Thread.sleep((long) remainingTime);

                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
