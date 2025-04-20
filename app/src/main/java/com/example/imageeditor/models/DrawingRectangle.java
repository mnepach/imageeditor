package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Matrix;
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
    public void transform(Matrix matrix) {
        float[] points = {startX, startY, endX, endY};
        matrix.mapPoints(points);
        startX = points[0];
        startY = points[1];
        endX = points[2];
        endY = points[3];
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float left = Math.min(startX, endX);
        float right = Math.max(startX, endX);
        float top = Math.min(startY, endY);
        float bottom = Math.max(startY, endY);
        float padding = paint.getStrokeWidth() + 10;
        return x >= left - padding && x <= right + padding &&
                y >= top - padding && y <= bottom + padding;
    }
}