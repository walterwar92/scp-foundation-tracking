package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class LogoAnimator {

    private final Canvas canvas;
    private double angle = 0;
    private AnimationTimer timer;

    public LogoAnimator(Canvas canvas) {
        this.canvas = canvas;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                angle += 0.4;
                render();
            }
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
        g.clearRect(0, 0, w, h);

        double cx = w / 2, cy = h / 2;
        double rOuter = Math.min(w, h) * 0.42;
        double rInner = rOuter * 0.55;

        // Внешний круг
        g.setStroke(Color.web("#FF6B35"));
        g.setLineWidth(2.5);
        g.strokeOval(cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2);

        // Три стрелки (вращаются)
        g.setFill(Color.web("#FF6B35"));
        for (int i = 0; i < 3; i++) {
            double theta = Math.toRadians(angle + i * 120);
            double[] xs = new double[3];
            double[] ys = new double[3];
            for (int k = 0; k < 3; k++) {
                double a = theta + k * Math.toRadians(40) - Math.toRadians(20);
                double r = (k == 1) ? rOuter * 0.85 : rInner;
                xs[k] = cx + Math.cos(a) * r;
                ys[k] = cy + Math.sin(a) * r;
            }
            g.fillPolygon(xs, ys, 3);
        }

        // Внутренний круг
        g.setStroke(Color.web("#C8102E"));
        g.setLineWidth(2);
        g.strokeOval(cx - rInner * 0.4, cy - rInner * 0.4, rInner * 0.8, rInner * 0.8);

        // Текст SCP в центре
        g.setFill(Color.web("#FF6B35"));
        g.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, rInner * 0.4));
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.fillText("SCP", cx, cy);
    }
}
