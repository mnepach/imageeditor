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
        float left = Math.min(startX, endX);
        float right = Math.max(startX, endX);
        float top = Math.min(startY, endY);
        float bottom = Math.max(startY, endY);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float left = Math.min(startX, endX);
        float right = Math.max(startX, endX);
        float top = Math.min(startY, endY);
        float bottom = Math.max(startY, endY);
        return x >= left && x <= right && y >= top && y <= bottom;
    }
}