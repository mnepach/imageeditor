package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.RectF;

public class DrawingCircle extends DrawingShape {

    public DrawingCircle(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF rect = new RectF(getLeft(), getTop(), getRight(), getBottom());
        canvas.drawOval(rect, paint);
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float centerX = (startX + endX) / 2;
        float centerY = (startY + endY) / 2;
        float radiusX = getWidth() / 2;
        float radiusY = getHeight() / 2;

        if (radiusX == 0 || radiusY == 0) {
            return false;
        }

        // Нормализация точки для проверки эллипса
        float normalizedX = (x - centerX) / radiusX;
        float normalizedY = (y - centerY) / radiusY;

        // Вычисляем расстояние от точки до границы эллипса
        float distance = normalizedX * normalizedX + normalizedY * normalizedY;

        float strokeWidth = paint.getStrokeWidth() / Math.min(radiusX, radiusY);

        // Проверяем, находится ли точка рядом с границей эллипса
        return Math.abs(distance - 1) <= strokeWidth / Math.min(radiusX, radiusY);
    }
}