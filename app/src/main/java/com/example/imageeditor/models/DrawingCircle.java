package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Paint;

public class DrawingCircle extends DrawingObject {

    public DrawingCircle(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
        this.paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.clipRect(0, 0, canvas.getWidth(), canvas.getHeight());
        float cx = (startX + endX) / 2;
        float cy = (startY + endY) / 2;
        float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.restore();
    }

    @Override
    public boolean containsPoint(float x, float y) {
        float cx = (startX + endX) / 2;
        float cy = (startY + endY) / 2;
        float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
        float distance = (float) Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
        return distance <= radius + paint.getStrokeWidth();
    }
}