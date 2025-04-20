package com.example.imageeditor.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.imageeditor.history.DrawCommand;
import com.example.imageeditor.history.HistoryManager;
import com.example.imageeditor.models.DrawingCircle;
import com.example.imageeditor.models.DrawingLine;
import com.example.imageeditor.models.DrawingObject;
import com.example.imageeditor.models.DrawingRectangle;
import com.example.imageeditor.models.DrawingText;

import java.util.ArrayList;
import java.util.List;

public class EditorView extends View {
    private static final String TAG = "EditorView";

    private Bitmap originalBitmap;
    private Bitmap workingBitmap;
    private Matrix imageMatrix;
    private Canvas bitmapCanvas;

    private int brushColor = 0xFF000000;
    private int brushSize = 5;

    private DrawingObject currentDrawingObject;
    private List<DrawingObject> drawingObjects = new ArrayList<>();
    private HistoryManager historyManager = new HistoryManager();

    private String drawingText = "";
    private String fontFamily = "sans-serif";
    private int textStyle = Typeface.NORMAL;
    private int textSize = 40;

    private RectF cropRect;
    private boolean cropMode = false;

    public enum DrawingMode {
        NONE, LINE, RECTANGLE, CIRCLE, TEXT
    }

    private DrawingMode currentDrawingMode = DrawingMode.NONE;

    public EditorView(Context context) {
        super(context);
        init();
    }

    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        imageMatrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (workingBitmap != null) {
            canvas.drawBitmap(workingBitmap, imageMatrix, null);
        }

        canvas.save();
        canvas.concat(imageMatrix);

        for (DrawingObject obj : drawingObjects) {
            obj.draw(canvas);
        }

        if (currentDrawingObject != null) {
            currentDrawingObject.draw(canvas);
        }

        canvas.restore();

        if (cropMode && cropRect != null) {
            Paint cropPaint = new Paint();
            cropPaint.setColor(0xFFFFFFFF);
            cropPaint.setStyle(Paint.Style.STROKE);
            cropPaint.setStrokeWidth(2f);
            canvas.drawRect(cropRect, cropPaint);

            float handleRadius = 10f;
            cropPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, cropPaint);
            canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, cropPaint);
            canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, cropPaint);
            canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, cropPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);
        float[] points = {x, y};
        inverseMatrix.mapPoints(points);
        float bitmapX = points[0];
        float bitmapY = points[1];

        // Ограничиваем координаты внутри изображения
        if (workingBitmap != null) {
            bitmapX = Math.max(0, Math.min(bitmapX, workingBitmap.getWidth()));
            bitmapY = Math.max(0, Math.min(bitmapY, workingBitmap.getHeight()));
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "Касание: (" + bitmapX + ", " + bitmapY + ")");
                if (cropMode) {
                    handleCropStart(x, y);
                } else {
                    handleDrawStart(bitmapX, bitmapY);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cropMode) {
                    handleCropMove(x, y);
                } else {
                    handleDrawMove(bitmapX, bitmapY);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (cropMode) {
                    handleCropEnd();
                } else {
                    handleDrawEnd();
                }
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public boolean isCropModeActive() {
        return cropMode;
    }

    private void handleDrawStart(float bitmapX, float bitmapY) {
        switch (currentDrawingMode) {
            case LINE:
                currentDrawingObject = new DrawingLine(bitmapX, bitmapY, brushColor, brushSize);
                Log.d(TAG, "Начато рисование линии");
                break;
            case RECTANGLE:
                currentDrawingObject = new DrawingRectangle(bitmapX, bitmapY, brushColor, brushSize);
                Log.d(TAG, "Начато рисование прямоугольника");
                break;
            case CIRCLE:
                currentDrawingObject = new DrawingCircle(bitmapX, bitmapY, brushColor, brushSize);
                Log.d(TAG, "Начато рисование круга");
                break;
            case TEXT:
                if (!drawingText.isEmpty()) {
                    currentDrawingObject = new DrawingText(bitmapX, bitmapY, drawingText, fontFamily, textStyle, textSize, brushColor);
                    Log.d(TAG, "Добавлен текст: " + drawingText + " на (" + bitmapX + ", " + bitmapY + ")");
                }
                break;
        }
    }

    private void handleDrawMove(float bitmapX, float bitmapY) {
        if (currentDrawingObject != null) {
            if (currentDrawingObject instanceof DrawingLine) {
                ((DrawingLine) currentDrawingObject).addPoint(bitmapX, bitmapY);
            } else {
                currentDrawingObject.updateEndPoint(bitmapX, bitmapY);
            }
        }
    }

    private void handleDrawEnd() {
        if (currentDrawingObject != null) {
            drawingObjects.add(currentDrawingObject);
            historyManager.executeCommand(new DrawCommand(drawingObjects, currentDrawingObject));
            Log.d(TAG, "Объект добавлен в историю: " + currentDrawingObject.getClass().getSimpleName());
            currentDrawingObject = null;
        }
    }

    private void handleCropStart(float x, float y) {
        if (workingBitmap == null) return;

        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);
        float[] points = {x, y};
        inverseMatrix.mapPoints(points);
        float bitmapX = Math.max(0, Math.min(points[0], workingBitmap.getWidth()));
        float bitmapY = Math.max(0, Math.min(points[1], workingBitmap.getHeight()));

        float[] bitmapPoints = {bitmapX, bitmapY};
        imageMatrix.mapPoints(bitmapPoints);
        float constrainedX = bitmapPoints[0];
        float constrainedY = bitmapPoints[1];

        cropRect = new RectF(constrainedX, constrainedY, constrainedX, constrainedY);
        Log.d(TAG, "Начата обрезка: (" + bitmapX + ", " + bitmapY + ")");
    }

    private void handleCropMove(float x, float y) {
        if (cropRect == null || workingBitmap == null) return;

        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);
        float[] points = {x, y};
        inverseMatrix.mapPoints(points);
        float bitmapX = Math.max(0, Math.min(points[0], workingBitmap.getWidth()));
        float bitmapY = Math.max(0, Math.min(points[1], workingBitmap.getHeight()));

        float[] bitmapPoints = {bitmapX, bitmapY};
        imageMatrix.mapPoints(bitmapPoints);
        float constrainedX = bitmapPoints[0];
        float constrainedY = bitmapPoints[1];

        cropRect.right = constrainedX;
        cropRect.bottom = constrainedY;
    }

    private void handleCropEnd() {
        if (cropRect != null) {
            float left = Math.min(cropRect.left, cropRect.right);
            float top = Math.min(cropRect.top, cropRect.bottom);
            float right = Math.max(cropRect.left, cropRect.right);
            float bottom = Math.max(cropRect.top, cropRect.bottom);
            cropRect.set(left, top, right, bottom);
            Log.d(TAG, "Обрезка завершена: " + cropRect.toString());
        }
    }

    public void applyCrop() {
        if (cropMode && cropRect != null && workingBitmap != null) {
            Matrix inverseMatrix = new Matrix();
            imageMatrix.invert(inverseMatrix);
            float[] points = {cropRect.left, cropRect.top, cropRect.right, cropRect.bottom};
            inverseMatrix.mapPoints(points);

            int x = (int) Math.max(Math.min(points[0], points[2]), 0);
            int y = (int) Math.max(Math.min(points[1], points[3]), 0);
            int width = (int) Math.min(Math.abs(points[2] - points[0]), workingBitmap.getWidth() - x);
            int height = (int) Math.min(Math.abs(points[3] - points[1]), workingBitmap.getHeight() - y);

            if (width > 0 && height > 0) {
                for (DrawingObject obj : drawingObjects) {
                    obj.draw(bitmapCanvas);
                }

                Bitmap croppedBitmap = Bitmap.createBitmap(workingBitmap, x, y, width, height);
                workingBitmap.recycle();
                workingBitmap = croppedBitmap;
                bitmapCanvas = new Canvas(workingBitmap);

                drawingObjects.clear();
                historyManager.clear();

                imageMatrix.reset();
                fitImageToView();

                cropMode = false;
                cropRect = null;
                invalidate();
                Log.d(TAG, "Обрезка применена: " + width + "x" + height);
            } else {
                Log.w(TAG, "Недопустимая область обрезки: " + width + "x" + height);
                cropRect = null;
            }
        } else {
            Log.w(TAG, "Обрезка не применена: cropMode=" + cropMode + ", cropRect=" + cropRect + ", workingBitmap=" + workingBitmap);
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            originalBitmap = bitmap;
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmapCanvas = new Canvas(workingBitmap);
            imageMatrix.reset();
            fitImageToView();
            invalidate();
            Log.d(TAG, "Установлено изображение: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        }
    }

    public void fitImageToView() {
        if (workingBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        imageMatrix.reset();
        float scaleX = (float) getWidth() / workingBitmap.getWidth();
        float scaleY = (float) getHeight() / workingBitmap.getHeight();
        float scale = Math.min(scaleX, scaleY);
        if (scale > 1.0f) {
            scale = 1.0f;
        }

        imageMatrix.setScale(scale, scale);
        float dx = (getWidth() - workingBitmap.getWidth() * scale) / 2f;
        float dy = (getHeight() - workingBitmap.getHeight() * scale) / 2f;
        imageMatrix.postTranslate(dx, dy);
        invalidate();
    }

    public void setDrawingMode(DrawingMode mode) {
        this.currentDrawingMode = mode;
        this.cropMode = false;
        Log.d(TAG, "Режим рисования: " + mode);
    }

    public void startCropMode() {
        cropMode = true;
        currentDrawingMode = DrawingMode.NONE;
        cropRect = null;
        Log.d(TAG, "Режим обрезки активирован");
    }

    public void rotateImage(int degrees) {
        if (workingBitmap == null) return;

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(degrees, workingBitmap.getWidth() / 2f, workingBitmap.getHeight() / 2f);

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                workingBitmap, 0, 0, workingBitmap.getWidth(), workingBitmap.getHeight(), rotateMatrix, true);

        if (rotatedBitmap != workingBitmap) {
            workingBitmap.recycle();
            workingBitmap = rotatedBitmap;
            bitmapCanvas = new Canvas(workingBitmap);
        }

        for (DrawingObject obj : drawingObjects) {
            float oldStartX = obj.getStartX();
            float oldStartY = obj.getStartY();
            float oldEndX = obj.getEndX();
            float oldEndY = obj.getEndY();
            float[] points = {oldStartX, oldStartY, oldEndX, oldEndY};
            rotateMatrix.mapPoints(points);
            obj.updateStartPoint(points[0], points[1]);
            obj.updateEndPoint(points[2], points[3]);
        }

        fitImageToView();
        invalidate();
        Log.d(TAG, "Изображение повернуто на " + degrees + " градусов");
    }

    public void flipImage() {
        if (workingBitmap == null) return;

        Matrix flipMatrix = new Matrix();
        flipMatrix.setScale(-1, 1);
        flipMatrix.postTranslate(workingBitmap.getWidth(), 0);

        Bitmap flippedBitmap = Bitmap.createBitmap(
                workingBitmap, 0, 0, workingBitmap.getWidth(), workingBitmap.getHeight(), flipMatrix, true);

        if (flippedBitmap != workingBitmap) {
            workingBitmap.recycle();
            workingBitmap = flippedBitmap;
            bitmapCanvas = new Canvas(workingBitmap);
        }

        // Отражаем координаты объектов рисования
        for (DrawingObject obj : drawingObjects) {
            float oldStartX = obj.getStartX();
            float oldStartY = obj.getStartY();
            float oldEndX = obj.getEndX();
            float oldEndY = obj.getEndY();
            // Пересчитываем координаты относительно ширины изображения
            float newStartX = workingBitmap.getWidth() - oldStartX;
            float newEndX = workingBitmap.getWidth() - oldEndX;
            obj.updateStartPoint(newStartX, oldStartY);
            obj.updateEndPoint(newEndX, oldEndY);
        }

        fitImageToView();
        invalidate();
        Log.d(TAG, "Изображение и рисунки отражены");
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
        Log.d(TAG, "Установлен размер кисти: " + size);
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
        Log.d(TAG, "Установлен цвет кисти: " + Integer.toHexString(color));
    }

    public void setTextDrawingProperties(String text, String fontFamily, int style, int textSize, int color) {
        this.drawingText = text;
        this.fontFamily = fontFamily;
        this.textStyle = style;
        this.textSize = textSize;
        this.brushColor = color;
        Log.d(TAG, "Установлены свойства текста: " + text + ", " + fontFamily + ", " + style + ", " + textSize);
    }

    public void undo() {
        historyManager.undo();
        invalidate();
        Log.d(TAG, "Выполнена отмена действия");
    }

    public void redo() {
        historyManager.redo();
        invalidate();
        Log.d(TAG, "Выполнено повторение действия");
    }

    public Bitmap getFinalBitmap() {
        if (workingBitmap == null) return null;

        Bitmap resultBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(), workingBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(workingBitmap, 0, 0, null);
        for (DrawingObject obj : drawingObjects) {
            obj.draw(canvas);
        }
        Log.d(TAG, "Создано финальное изображение");
        return resultBitmap;
    }
}