package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.RectF;

public class DrawingRectangle extends DrawingShape {

    public DrawingRectangle(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF rect = new RectF(getLeft(), getTop(), getRight(), getBottom());
        canvas.drawRect(rect, paint);
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float strokeWidth = paint.getStrokeWidth();
        float left = getLeft() - strokeWidth / 2;
        float top = getTop() - strokeWidth / 2;
        float right = getRight() + strokeWidth / 2;
        float bottom = getBottom() + strokeWidth / 2;

        // Проверяем, находится ли точка рядом с границами прямоугольника
        boolean nearTopEdge = y >= top - strokeWidth && y <= top + strokeWidth && x >= left && x <= right;
        boolean nearBottomEdge = y >= bottom - strokeWidth && y <= bottom + strokeWidth && x >= left && x <= right;
        boolean nearLeftEdge = x >= left - strokeWidth && x <= left + strokeWidth && y >= top && y <= bottom;
        boolean nearRightEdge = x >= right - strokeWidth && x <= right + strokeWidth && y >= top && y <= bottom;

        return nearTopEdge || nearBottomEdge || nearLeftEdge || nearRightEdge;
    }
}