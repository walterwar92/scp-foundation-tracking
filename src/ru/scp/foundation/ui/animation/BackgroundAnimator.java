package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Анимация фона: движущиеся частицы + горизонтальные сканлайны.
 * Эффект "интерфейс из шпионского триллера".
 */
public class BackgroundAnimator {

    private static final int PARTICLES = 80;
    private final Canvas canvas;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();
    private double scanLineY = 0;
    private AnimationTimer timer;

    public BackgroundAnimator(Canvas canvas) {
        this.canvas = canvas;
        for (int i = 0; i < PARTICLES; i++) particles.add(new Particle());
    }

    public void start() {
        for (Particle p : particles) p.reset();
        timer = new AnimationTimer() {
            @Override public void handle(long now) { render(); }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w == 0 || h == 0) return;
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Затемнение прошлого кадра (создаёт шлейф)
        g.setFill(Color.color(0.04, 0.04, 0.04, 0.25));
        g.fillRect(0, 0, w, h);

        // Сетка
        g.setStroke(Color.color(1.0, 0.42, 0.21, 0.06));
        g.setLineWidth(1);
        for (int x = 0; x < w; x += 40) g.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) g.strokeLine(0, y, w, y);

        // Частицы
        for (Particle p : particles) {
            p.update(w, h);
            g.setFill(Color.color(1.0, 0.42, 0.21, p.alpha));
            g.fillOval(p.x, p.y, p.size, p.size);
        }

        // Сканлайн
        scanLineY += 1.5;
        if (scanLineY > h) scanLineY = 0;
        g.setStroke(Color.color(0.78, 0.06, 0.18, 0.4));
        g.setLineWidth(2);
        g.strokeLine(0, scanLineY, w, scanLineY);
    }

    private class Particle {
        double x, y, vx, vy, size, alpha;
        Particle() { reset(); }
        void reset() {
            x = rng.nextDouble() * Math.max(1, canvas.getWidth());
            y = rng.nextDouble() * Math.max(1, canvas.getHeight());
            vx = (rng.nextDouble() - 0.5) * 0.6;
            vy = (rng.nextDouble() - 0.5) * 0.6;
            size = 1 + rng.nextDouble() * 2.5;
            alpha = 0.25 + rng.nextDouble() * 0.5;
        }
        void update(double w, double h) {
            x += vx; y += vy;
            if (x < 0) x = w; if (x > w) x = 0;
            if (y < 0) y = h; if (y > h) y = 0;
        }
    }
}
