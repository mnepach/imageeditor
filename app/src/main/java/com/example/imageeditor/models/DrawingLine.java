package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;

public class DrawingLine extends DrawingObject {
    private Path path;

    public DrawingLine(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);

        this.path = new Path();
        this.path.moveTo(startX, startY);
        this.paint.setStyle(Paint.Style.STROKE);
    }

    public void addPoint(float x, float y) {
        this.path.lineTo(x, y);
        this.endX = x;
        this.endY = y;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean containsPoint(float x, float y) {
        // Для линий проверять труднее, поэтому просто используем базовую реализацию
        return super.containsPoint(x, y);
    }
}