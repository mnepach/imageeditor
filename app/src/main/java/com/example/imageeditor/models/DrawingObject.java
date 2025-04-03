package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class DrawingObject {
    protected float startX;
    protected float startY;
    protected float endX;
    protected float endY;
    protected Paint paint;

    public DrawingObject(float startX, float startY, int color, int strokeWidth) {
        this.startX = startX;
        this.startY = startY;
        this.endX = startX;
        this.endY = startY;

        this.paint = new Paint();
        this.paint.setColor(color);
        this.paint.setStrokeWidth(strokeWidth);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setAntiAlias(true);
    }

    public void updateEndPoint(float endX, float endY) {
        this.endX = endX;
        this.endY = endY;
    }

    public abstract void draw(Canvas canvas);

    public boolean containsPoint(float x, float y) {
        // Базовая проверка, переопределяется в наследниках
        float padding = paint.getStrokeWidth() + 10;
        return x >= Math.min(startX, endX) - padding &&
                x <= Math.max(startX, endX) + padding &&
                y >= Math.min(startY, endY) - padding &&
                y <= Math.max(startY, endY) + padding;
    }
}