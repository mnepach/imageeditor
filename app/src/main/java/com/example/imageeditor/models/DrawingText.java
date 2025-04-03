package com.example.imageeditor.models;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class DrawingText extends DrawingObject {
    private String text;
    private String fontFamily;
    private int textStyle;
    private int textSize;

    public DrawingText(float x, float y, String text, String fontFamily, int textStyle, int textSize, int color) {
        super(x, y, color, 1); // strokeWidth не используется для текста
        this.text = text;
        this.fontFamily = fontFamily;
        this.textStyle = textStyle;
        this.textSize = textSize;

        // Настраиваем Paint для текста
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setTextSize(textSize);
        this.paint.setTypeface(Typeface.create(fontFamily, textStyle));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawText(text, startX, startY, paint);
    }

    @Override
    public boolean containsPoint(float x, float y) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        bounds.offset((int)startX, (int)startY);

        // Учитываем, что baseline текста находится внизу, поэтому смещаем область вверх
        bounds.top -= paint.getTextSize();

        // Добавляем padding для удобства касания
        int padding = 20;
        bounds.inset(-padding, -padding);

        return bounds.contains((int)x, (int)y);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        updateTypeface();
    }

    public void setTextStyle(int textStyle) {
        this.textStyle = textStyle;
        updateTypeface();
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        this.paint.setTextSize(textSize);
    }

    private void updateTypeface() {
        this.paint.setTypeface(Typeface.create(fontFamily, textStyle));
    }
}