package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.PathMeasure;

import java.util.ArrayList;
import java.util.List;

public class DrawingLine extends DrawingObject {
    private Path path;
    private List<PointF> points;

    public static class PointF {
        public float x, y;

        public PointF(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public DrawingLine(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
        this.path = new Path();
        this.path.moveTo(startX, startY);
        this.paint.setStyle(Paint.Style.STROKE);

        this.points = new ArrayList<>();
        this.points.add(new PointF(startX, startY));
    }

    public void addPoint(float x, float y) {
        this.path.lineTo(x, y);
        this.points.add(new PointF(x, y));
        this.endX = x;
        this.endY = y;
    }

    @Override
    public void draw(Canvas canvas) {
        // Перестраиваем путь из точек для точного рендеринга
        Path drawPath = new Path();
        if (!points.isEmpty()) {
            drawPath.moveTo(points.get(0).x, points.get(0).y);
            for (int i = 1; i < points.size(); i++) {
                drawPath.lineTo(points.get(i).x, points.get(i).y);
            }
        }
        canvas.drawPath(drawPath, paint);
    }

    @Override
    public void transform(Matrix matrix) {
        // Трансформируем все точки
        path.reset();
        boolean first = true;

        for (PointF point : points) {
            float[] pt = {point.x, point.y};
            matrix.mapPoints(pt);
            point.x = pt[0];
            point.y = pt[1];

            if (first) {
                path.moveTo(point.x, point.y);
                startX = point.x;
                startY = point.y;
                first = false;
            } else {
                path.lineTo(point.x, point.y);
            }
        }

        if (!points.isEmpty()) {
            PointF lastPoint = points.get(points.size() - 1);
            endX = lastPoint.x;
            endY = lastPoint.y;
        }
    }

    @Override
    public boolean containsPoint(float x, float y) {
        // Проверка близости точки к любому сегменту пути
        float threshold = paint.getStrokeWidth() + 10;

        for (int i = 0; i < points.size() - 1; i++) {
            PointF p1 = points.get(i);
            PointF p2 = points.get(i + 1);

            // Расстояние от точки до отрезка
            float distance = distanceToSegment(x, y, p1.x, p1.y, p2.x, p2.y);
            if (distance <= threshold) {
                return true;
            }
        }

        return false;
    }

    // Метод для расчета расстояния от точки до отрезка
    private float distanceToSegment(float x, float y, float x1, float y1, float x2, float y2) {
        float A = x - x1;
        float B = y - y1;
        float C = x2 - x1;
        float D = y2 - y1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;
        if (len_sq != 0) {
            param = dot / len_sq;
        }

        float xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        float dx = x - xx;
        float dy = y - yy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}