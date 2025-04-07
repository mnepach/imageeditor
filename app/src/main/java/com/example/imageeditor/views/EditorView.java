package com.example.imageeditor.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.imageeditor.models.DrawingCircle;
import com.example.imageeditor.models.DrawingLine;
import com.example.imageeditor.models.DrawingRectangle;
import com.example.imageeditor.models.DrawingText;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorView extends View {

    // Drawing modes
    public enum DrawingMode {
        NONE, LINE, RECTANGLE, CIRCLE, TEXT
    }

    private DrawingMode currentDrawingMode = DrawingMode.NONE;
    private boolean cropMode = false;

    // Original image and working copy
    private Bitmap originalBitmap;
    private Bitmap workingBitmap;
    private Matrix imageMatrix;

    // Drawing parameters
    private Paint paint;
    private int brushColor = 0xFF000000; // Black by default
    private float brushSize = 5f;

    // For drawing lines
    private Path currentPath;
    private float lastX, lastY;

    // For drawing shapes
    private float startX, startY;

    // For text drawing
    private String drawingText = "";
    private String fontFamily = "sans-serif";
    private int textStyle = Typeface.NORMAL;
    private float textSize = 40f;
    private int textColor = 0xFF000000;

    // Crop rectangle
    private RectF cropRect;

    // Lists of drawing objects and operations
    private List<Object> drawingObjects = new ArrayList<>();
    private Stack<Object> undoStack = new Stack<>();
    private Stack<Object> redoStack = new Stack<>();

    // Constructors
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
        // Initialize paint
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(brushColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(brushSize);

        // Initialize matrix
        imageMatrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        canvas.drawARGB(255, 240, 240, 240);

        // Draw bitmap if available
        if (workingBitmap != null) {
            canvas.drawBitmap(workingBitmap, imageMatrix, null);
        }

        // Draw all existing objects
        for (Object obj : drawingObjects) {
            if (obj instanceof DrawingLine) {
                DrawingLine line = (DrawingLine) obj;
                Paint linePaint = new Paint(paint);
                linePaint.setColor(line.getColor());
                linePaint.setStrokeWidth(line.getStrokeWidth());
                canvas.drawPath(line.getPath(), linePaint);
            } else if (obj instanceof DrawingRectangle) {
                DrawingRectangle rect = (DrawingRectangle) obj;
                Paint rectPaint = new Paint(paint);
                rectPaint.setColor(rect.getColor());
                rectPaint.setStrokeWidth(rect.getStrokeWidth());
                canvas.drawRect(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), rectPaint);
            } else if (obj instanceof DrawingCircle) {
                DrawingCircle circle = (DrawingCircle) obj;
                Paint circlePaint = new Paint(paint);
                circlePaint.setColor(circle.getColor());
                circlePaint.setStrokeWidth(circle.getStrokeWidth());
                canvas.drawCircle(circle.getCenterX(), circle.getCenterY(), circle.getRadius(), circlePaint);
            } else if (obj instanceof DrawingText) {
                DrawingText text = (DrawingText) obj;
                Paint textPaint = new Paint();
                textPaint.setAntiAlias(true);
                textPaint.setColor(text.getColor());
                textPaint.setTextSize(text.getTextSize());
                textPaint.setTypeface(Typeface.create(text.getFontFamily(), text.getStyle()));
                canvas.drawText(text.getText(), text.getX(), text.getY(), textPaint);
            }
        }

        // Draw current path if drawing
        if (currentDrawingMode == DrawingMode.LINE && currentPath != null) {
            canvas.drawPath(currentPath, paint);
        }

        // Draw crop rectangle if in crop mode
        if (cropMode && cropRect != null) {
            Paint cropPaint = new Paint();
            cropPaint.setColor(0xFFFFFFFF);
            cropPaint.setStyle(Paint.Style.STROKE);
            cropPaint.setStrokeWidth(2f);
            canvas.drawRect(cropRect, cropPaint);

            // Draw handles at corners
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

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (cropMode) {
                    handleCropStart(x, y);
                } else {
                    handleDrawStart(x, y);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cropMode) {
                    handleCropMove(x, y);
                } else {
                    handleDrawMove(x, y);
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

    private void handleDrawStart(float x, float y) {
        lastX = x;
        lastY = y;

        switch (currentDrawingMode) {
            case LINE:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                break;
            case RECTANGLE:
            case CIRCLE:
                startX = x;
                startY = y;
                break;
            case TEXT:
                // For text, we just store the position. Actual text addition is handled elsewhere
                if (!drawingText.isEmpty()) {
                    addTextAtPosition(x, y);
                }
                break;
        }
    }

    private void handleDrawMove(float x, float y) {
        switch (currentDrawingMode) {
            case LINE:
                if (currentPath != null) {
                    currentPath.quadTo(
                            lastX, lastY,
                            (x + lastX) / 2, (y + lastY) / 2);
                    lastX = x;
                    lastY = y;
                }
                break;
        }
    }

    private void handleDrawEnd() {
        switch (currentDrawingMode) {
            case LINE:
                if (currentPath != null) {
                    DrawingLine line = new DrawingLine(new Path(currentPath), brushColor, brushSize);
                    drawingObjects.add(line);
                    undoStack.push(line);
                    redoStack.clear();
                    currentPath = null;
                }
                break;
            case RECTANGLE:
                float left = Math.min(startX, lastX);
                float top = Math.min(startY, lastY);
                float right = Math.max(startX, lastX);
                float bottom = Math.max(startY, lastY);

                DrawingRectangle rect = new DrawingRectangle(left, top, right, bottom, brushColor, brushSize);
                drawingObjects.add(rect);
                undoStack.push(rect);
                redoStack.clear();
                break;
            case CIRCLE:
                float centerX = (startX + lastX) / 2;
                float centerY = (startY + lastY) / 2;
                float radius = (float) Math.sqrt(Math.pow(centerX - startX, 2) + Math.pow(centerY - startY, 2));

                DrawingCircle circle = new DrawingCircle(centerX, centerY, radius, brushColor, brushSize);
                drawingObjects.add(circle);
                undoStack.push(circle);
                redoStack.clear();
                break;
        }
    }

    private void handleCropStart(float x, float y) {
        if (cropRect == null) {
            cropRect = new RectF(x, y, x, y);
        }
    }

    private void handleCropMove(float x, float y) {
        if (cropRect != null) {
            cropRect.right = x;
            cropRect.bottom = y;
        }
    }

    private void handleCropEnd() {
        // Normalize crop rectangle (make sure left < right and top < bottom)
        if (cropRect != null) {
            if (cropRect.width() < 10 || cropRect.height() < 10) {
                // If too small, cancel crop
                cropRect = null;
                return;
            }

            // Otherwise, finalize crop rectangle
            float left = Math.min(cropRect.left, cropRect.right);
            float top = Math.min(cropRect.top, cropRect.bottom);
            float right = Math.max(cropRect.left, cropRect.right);
            float bottom = Math.max(cropRect.top, cropRect.bottom);

            cropRect.set(left, top, right, bottom);
        }
    }

    private void addTextAtPosition(float x, float y) {
        DrawingText text = new DrawingText(drawingText, x, y, textColor, fontFamily, textStyle, textSize);
        drawingObjects.add(text);
        undoStack.push(text);
        redoStack.clear();
    }

    // Public methods for EditorActivity to use

    public void setImageBitmap(Bitmap bitmap) {
        originalBitmap = bitmap;
        workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        fitImageToView();
        invalidate();
    }

    private void fitImageToView() {
        if (workingBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        imageMatrix.reset();

        float scaleX = (float) getWidth() / workingBitmap.getWidth();
        float scaleY = (float) getHeight() / workingBitmap.getHeight();
        float scale = Math.min(scaleX, scaleY);

        float dx = (getWidth() - workingBitmap.getWidth() * scale) / 2f;
        float dy = (getHeight() - workingBitmap.getHeight() * scale) / 2f;

        imageMatrix.setScale(scale, scale);
        imageMatrix.postTranslate(dx, dy);
    }

    public void setDrawingMode(DrawingMode mode) {
        this.currentDrawingMode = mode;
        this.cropMode = false;
    }

    public void startCropMode() {
        cropMode = true;
        currentDrawingMode = DrawingMode.NONE;
        cropRect = null;
    }

    public void rotateImage(int degrees) {
        if (workingBitmap == null) return;

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(degrees, workingBitmap.getWidth() / 2f, workingBitmap.getHeight() / 2f);

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                workingBitmap,
                0, 0,
                workingBitmap.getWidth(), workingBitmap.getHeight(),
                rotateMatrix, true);

        if (rotatedBitmap != workingBitmap) {
            workingBitmap.recycle();
            workingBitmap = rotatedBitmap;
        }

        fitImageToView();
        invalidate();
    }

    public void flipImage() {
        if (workingBitmap == null) return;

        Matrix flipMatrix = new Matrix();
        flipMatrix.setScale(-1, 1);
        flipMatrix.postTranslate(workingBitmap.getWidth(), 0);

        Bitmap flippedBitmap = Bitmap.createBitmap(
                workingBitmap,
                0, 0,
                workingBitmap.getWidth(), workingBitmap.getHeight(),
                flipMatrix, true);

        if (flippedBitmap != workingBitmap) {
            workingBitmap.recycle();
            workingBitmap = flippedBitmap;
        }

        fitImageToView();
        invalidate();
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
        paint.setStrokeWidth(size);
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
        paint.setColor(color);
    }

    public void setTextDrawingProperties(String text, String fontFamily, int style, int textSize, int color) {
        this.drawingText = text;
        this.fontFamily = fontFamily;
        this.textStyle = style;
        this.textSize = textSize;
        this.textColor = color;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Object lastDrawn = undoStack.pop();
            redoStack.push(lastDrawn);

            // Rebuild drawing objects list
            drawingObjects.clear();
            for (Object obj : undoStack) {
                drawingObjects.add(obj);
            }

            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Object redoObj = redoStack.pop();
            undoStack.push(redoObj);
            drawingObjects.add(redoObj);

            invalidate();
        }
    }

    public Bitmap getFinalBitmap() {
        if (workingBitmap == null) return null;

        // Create a new bitmap to draw everything on
        Bitmap resultBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(),
                workingBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resultBitmap);

        // Draw the original image first
        canvas.drawBitmap(workingBitmap, 0, 0, null);

        // Invert the image matrix to map drawing coordinates back to bitmap space
        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);

        // Draw all objects onto the result bitmap
        for (Object obj : drawingObjects) {
            if (obj instanceof DrawingLine) {
                DrawingLine line = (DrawingLine) obj;
                Paint linePaint = new Paint(paint);
                linePaint.setColor(line.getColor());
                linePaint.setStrokeWidth(line.getStrokeWidth());

                // Transform the path
                Path transformedPath = new Path(line.getPath());
                transformedPath.transform(inverseMatrix);

                canvas.drawPath(transformedPath, linePaint);
            } else if (obj instanceof DrawingRectangle) {
                DrawingRectangle rect = (DrawingRectangle) obj;
                Paint rectPaint = new Paint(paint);
                rectPaint.setColor(rect.getColor());
                rectPaint.setStrokeWidth(rect.getStrokeWidth());

                // Transform the rectangle
                RectF transformedRect = new RectF(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
                inverseMatrix.mapRect(transformedRect);

                canvas.drawRect(transformedRect, rectPaint);
            } else if (obj instanceof DrawingCircle) {
                DrawingCircle circle = (DrawingCircle) obj;
                Paint circlePaint = new Paint(paint);
                circlePaint.setColor(circle.getColor());
                circlePaint.setStrokeWidth(circle.getStrokeWidth());

                // Transform the circle
                float[] center = new float[] {circle.getCenterX(), circle.getCenterY()};
                inverseMatrix.mapPoints(center);

                // Scale the radius - this is an approximation
                float[] values = new float[9];
                inverseMatrix.getValues(values);
                float scaleX = values[Matrix.MSCALE_X];
                float scaleY = values[Matrix.MSCALE_Y];
                float scaledRadius = circle.getRadius() / Math.max(Math.abs(scaleX), Math.abs(scaleY));

                canvas.drawCircle(center[0], center[1], scaledRadius, circlePaint);
            } else if (obj instanceof DrawingText) {
                DrawingText text = (DrawingText) obj;
                Paint textPaint = new Paint();
                textPaint.setAntiAlias(true);
                textPaint.setColor(text.getColor());
                textPaint.setTextSize(text.getTextSize());
                textPaint.setTypeface(Typeface.create(text.getFontFamily(), text.getStyle()));

                // Transform the position
                float[] position = new float[] {text.getX(), text.getY()};
                inverseMatrix.mapPoints(position);

                canvas.drawText(text.getText(), position[0], position[1], textPaint);
            }
        }

        return resultBitmap;
    }
}