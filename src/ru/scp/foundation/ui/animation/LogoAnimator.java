package ru.scp.foundation.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

/**
 * SCP-логотип в стиле web-дизайна:
 *   — внешний кольцевой обвод
 *   — пунктирное кольцо чуть внутри
 *   — три «стрелки» (дуга + треугольник наконечника), повёрнутые на 0/120/240°
 *   — внутренний полупрозрачный круг
 *   — статичный центральный текст «SCP» с оранжевым свечением
 * Кольца и стрелки вращаются вместе (~20°/сек, период 18 сек),
 * текст не вращается — рисуется в самом конце поверх.
 */
public class LogoAnimator {

    private static final Color ORANGE = Color.web("#FF6B35");
    private static final Color ORANGE_DIM = Color.web("#FF6B35", 0.55);
    private static final Color ORANGE_FAINT = Color.web("#FF6B35", 0.40);
    private static final Color ORANGE_GHOST = Color.web("#FF6B35", 0.35);

    private final Canvas canvas;
    private double angle = 0;
    private AnimationTimer timer;

    public LogoAnimator(Canvas canvas) {
        this.canvas = canvas;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                angle = (angle + 20.0 / 60.0) % 360.0;  // ~20°/sec @ 60fps
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

        // Канва web-логотипа 160×160 — масштабируем относительно нашей.
        double scale = Math.min(w, h) / 160.0;
        double cx = w / 2.0, cy = h / 2.0;

        g.save();
        g.translate(cx, cy);
        g.rotate(angle);  // вращаем кольца + стрелки как единое целое

        // Внешний сплошной круг (r = 74 в web → 74*scale тут)
        g.setStroke(ORANGE_DIM);
        g.setLineWidth(1.2 * scale);
        double rOuter = 74 * scale;
        g.strokeOval(-rOuter, -rOuter, 2 * rOuter, 2 * rOuter);

        // Пунктирный круг (r = 68)
        g.setStroke(ORANGE_FAINT);
        g.setLineWidth(0.6 * scale);
        g.setLineDashes(2 * scale, 4 * scale);
        double rDash = 68 * scale;
        g.strokeOval(-rDash, -rDash, 2 * rDash, 2 * rDash);
        g.setLineDashes(0);

        // Три стрелки на 0°/120°/240°
        g.setStroke(ORANGE);
        g.setFill(ORANGE);
        g.setLineWidth(3 * scale);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        DropShadow glow = new DropShadow(2 * scale, ORANGE);
        g.setEffect(glow);

        for (int i = 0; i < 3; i++) {
            g.save();
            g.rotate(i * 120);
            // Дуга от (0,-66) до (57,-33) по кругу радиуса 66
            // Делаем приблизительно через цепочку коротких линий по дуге.
            drawArc(g, 66 * scale, -90, -60, 24);
            // Наконечник: треугольник у вершины дуги (cx=0, cy=-74)
            double[] xs = { 0, -10 * scale,  10 * scale };
            double[] ys = { -74 * scale, -58 * scale, -58 * scale };
            g.fillPolygon(xs, ys, 3);
            g.restore();
        }
        g.setEffect(null);

        // Внутреннее тонкое кольцо (r = 42)
        g.setStroke(ORANGE_GHOST);
        g.setLineWidth(0.8 * scale);
        double rInner = 42 * scale;
        g.strokeOval(-rInner, -rInner, 2 * rInner, 2 * rInner);

        g.restore();  // конец вращающейся группы

        // Центральный текст SCP — не вращается, рисуется поверх
        double textSize = Math.max(14, 34 * scale * 0.9);
        g.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, textSize));
        g.setFill(ORANGE);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setEffect(new DropShadow(14 * scale, Color.web("#FF6B35", 0.6)));
        g.fillText("SCP", cx, cy);
        g.setEffect(null);
    }

    /**
     * Аппроксимация дуги короткими линиями. center=(0,0), радиус r,
     * углы fromDeg→toDeg (math-сист., -90 = верх), steps сегментов.
     */
    private static void drawArc(GraphicsContext g, double r, double fromDeg, double toDeg, int steps) {
        g.beginPath();
        for (int i = 0; i <= steps; i++) {
            double t = fromDeg + (toDeg - fromDeg) * i / steps;
            double rad = Math.toRadians(t);
            double x = Math.cos(rad) * r;
            double y = Math.sin(rad) * r;
            if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
        }
        g.stroke();
    }
}
