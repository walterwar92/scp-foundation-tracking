package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Многослойный анимированный фон в стиле SCP-терминала.
 *
 * Слои (рисуются снизу вверх):
 *   1) Базовая радиальная подсветка (оранжевый сверху-слева + красный снизу-справа)
 *   2) Сетка 40×40 px с прозрачностью
 *   3) Облако точек-частиц (drift)
 *   4) Бегущая горизонтальная сканлайн с дроп-шадоу
 *   5) Виньетка по краям
 *   6) Тонкие горизонтальные CRT-линии
 */
public class BackgroundAnimator {

    private static final int PARTICLES = 60;
    private static final double SCANLINE_SPEED = 1.4;
    private static final double SCANLINE_HEIGHT = 2.0;

    private final Canvas canvas;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng;
    private double scanLineY = 0;
    private AnimationTimer timer;

    public BackgroundAnimator(Canvas canvas) {
        this(canvas, 1L);
    }

    public BackgroundAnimator(Canvas canvas, long seed) {
        this.canvas = canvas;
        this.rng = new Random(seed);
        for (int i = 0; i < PARTICLES; i++) particles.add(new Particle());
    }

    public void start() {
        for (Particle p : particles) p.reset();
        scanLineY = -SCANLINE_HEIGHT;
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

        // 1. Радиальная подсветка как базовый фон
        g.setFill(Color.web("#0a0a0a"));
        g.fillRect(0, 0, w, h);

        RadialGradient orangeGlow = new RadialGradient(
            0, 0, 0.30, 0.20, Math.max(w, h) * 0.6, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FF6B35", 0.04)),
            new Stop(1, Color.web("#FF6B35", 0.0))
        );
        g.setFill(orangeGlow);
        g.fillRect(0, 0, w, h);

        RadialGradient redGlow = new RadialGradient(
            0, 0, 0.80, 0.90, Math.max(w, h) * 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#C8102E", 0.05)),
            new Stop(1, Color.web("#C8102E", 0.0))
        );
        g.setFill(redGlow);
        g.fillRect(0, 0, w, h);

        // 2. Сетка 40×40
        g.setStroke(Color.web("#FF6B35", 0.06));
        g.setLineWidth(1);
        for (double x = 0; x < w; x += 40) g.strokeLine(x, 0, x, h);
        for (double y = 0; y < h; y += 40) g.strokeLine(0, y, w, y);

        // 3. Частицы
        DropShadow particleGlow = new DropShadow(4, Color.web("#FF6B35"));
        g.setEffect(particleGlow);
        for (Particle p : particles) {
            p.update(w, h);
            g.setFill(Color.web("#FF6B35", p.alpha));
            g.fillOval(p.x, p.y, p.size, p.size);
        }
        g.setEffect(null);

        // 4. Сканлайн
        scanLineY += SCANLINE_SPEED;
        if (scanLineY > h + 20) scanLineY = -SCANLINE_HEIGHT;
        if (scanLineY >= 0 && scanLineY <= h) {
            g.setEffect(new DropShadow(12, Color.web("#C8102E", 0.6)));
            g.setFill(Color.web("#C8102E", 0.45));
            g.fillRect(0, scanLineY, w, SCANLINE_HEIGHT);
            g.setEffect(null);
        }

        // 5. Виньетка
        RadialGradient vignette = new RadialGradient(
            0, 0, 0.5, 0.5, Math.max(w, h) * 0.75, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(0, 0, 0, 0)),
            new Stop(0.55, Color.color(0, 0, 0, 0)),
            new Stop(1, Color.color(0, 0, 0, 0.65))
        );
        g.setFill(vignette);
        g.fillRect(0, 0, w, h);

        // 6. CRT-линии
        g.setStroke(Color.color(1, 1, 1, 0.02));
        g.setLineWidth(1);
        for (double y = 0; y < h; y += 3) g.strokeLine(0, y, w, y);
    }

    private class Particle {
        double x, y, vx, vy, size, alpha;

        Particle() { reset(); }

        void reset() {
            double w = Math.max(1, canvas.getWidth());
            double h = Math.max(1, canvas.getHeight());
            x = rng.nextDouble() * w;
            y = rng.nextDouble() * h;
            vx = (rng.nextDouble() - 0.5) * 0.5;
            vy = (rng.nextDouble() - 0.5) * 0.5;
            size = 1 + rng.nextDouble() * 2;
            alpha = 0.25 + rng.nextDouble() * 0.5;
        }

        void update(double w, double h) {
            x += vx; y += vy;
            if (x < 0) x = w;
            if (x > w) x = 0;
            if (y < 0) y = h;
            if (y > h) y = 0;
        }
    }
}
