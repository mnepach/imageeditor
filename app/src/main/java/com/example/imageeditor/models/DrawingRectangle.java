package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Paint;

public class DrawingRectangle extends DrawingObject {

    public DrawingRectangle(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
        this.paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.clipRect(0, 0, canvas.getWidth(), canvas.getHeight());
        float left = Math.min(startX, endX);
        float right = Math.max(startX, endX);
        float top = Math.min(startY, endY);
        float bottom = Math.max(startY, endY);
        canvas.drawRect(left, top, right, bottom, paint);
        canvas.restore();
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float left = Math.min(startX, endX);
        float right = Math.max(startX, endX);
        float top = Math.min(startY, endY);
        float bottom = Math.max(startY, endY);
        float padding = paint.getStrokeWidth() + 10;
        return x >= left - padding && x <= right + padding && y >= top - padding && y <= bottom + padding;
    }
}