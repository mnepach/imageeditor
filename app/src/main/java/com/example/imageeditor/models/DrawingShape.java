package com.example.imageeditor.models;

import android.graphics.Canvas;

public abstract class DrawingShape extends DrawingObject {

    public DrawingShape(float startX, float startY, int color, int strokeWidth) {
        super(startX, startY, color, strokeWidth);
    }

    @Override
    public void updateEndPoint(float endX, float endY) {
        super.updateEndPoint(endX, endY);
    }

    protected float getLeft() {
        return Math.min(startX, endX);
    }

    protected float getTop() {
        return Math.min(startY, endY);
    }

    protected float getRight() {
        return Math.max(startX, endX);
    }

    protected float getBottom() {
        return Math.max(startY, endY);
    }

    protected float getWidth() {
        return Math.abs(endX - startX);
    }

    protected float getHeight() {
        return Math.abs(endY - startY);
    }
}