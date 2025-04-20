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
import com.example.imageeditor.models.DrawingObject;
import com.example.imageeditor.models.DrawingRectangle;
import com.example.imageeditor.models.DrawingText;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorView extends View {

    private DrawingObject selectedObject = null;
    private boolean isDragging = false;
    private float lastTouchX, lastTouchY;
    private float[] lastTouchPoint = new float[2]; // For storing bitmap coordinates

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
    private int brushColor = 0xFF000000; // Black by default
    private int brushSize = 5;

    // For drawing objects
    private DrawingObject currentDrawingObject;

    // For text drawing
    private String drawingText = "";
    private String fontFamily = "sans-serif";
    private int textStyle = Typeface.NORMAL;
    private int textSize = 40;

    // Crop rectangle
    private RectF cropRect;

    // Lists of drawing objects and operations
    private List<DrawingObject> drawingObjects = new ArrayList<>();
    private Stack<DrawingObject> undoStack = new Stack<>();
    private Stack<DrawingObject> redoStack = new Stack<>();

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
        // Initialize matrix
        imageMatrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw bitmap if available
        if (workingBitmap != null) {
            canvas.drawBitmap(workingBitmap, imageMatrix, null);
        }

        // Save canvas state and apply imageMatrix for objects
        canvas.save();
        canvas.concat(imageMatrix);

        // Draw all existing objects in bitmap coordinates
        for (DrawingObject obj : drawingObjects) {
            obj.draw(canvas);
        }

        // Draw current object if it exists
        if (currentDrawingObject != null) {
            currentDrawingObject.draw(canvas);
        }

        canvas.restore();

        // Draw crop rectangle in screen coordinates
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

        // Convert screen coordinates to bitmap coordinates
        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);
        float[] points = {x, y};
        inverseMatrix.mapPoints(points);
        float bitmapX = points[0];
        float bitmapY = points[1];

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (cropMode) {
                    handleCropStart(x, y);
                } else {
                    // Check if click is on existing object
                    selectedObject = null;
                    for (int i = drawingObjects.size() - 1; i >= 0; i--) {
                        DrawingObject obj = drawingObjects.get(i);
                        if (obj.containsPoint(bitmapX, bitmapY)) {
                            selectedObject = obj;
                            isDragging = true;
                            lastTouchPoint[0] = bitmapX;
                            lastTouchPoint[1] = bitmapY;
                            break;
                        }
                    }
                    if (selectedObject == null) {
                        handleDrawStart(bitmapX, bitmapY);
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cropMode) {
                    handleCropMove(x, y);
                } else if (isDragging && selectedObject != null) {
                    float dx = bitmapX - lastTouchPoint[0];
                    float dy = bitmapY - lastTouchPoint[1];

                    selectedObject.updateStartPoint(selectedObject.getStartX() + dx, selectedObject.getStartY() + dy);
                    selectedObject.updateEndPoint(selectedObject.getEndX() + dx, selectedObject.getEndY() + dy);

                    lastTouchPoint[0] = bitmapX;
                    lastTouchPoint[1] = bitmapY;
                } else {
                    handleDrawMove(bitmapX, bitmapY);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (cropMode) {
                    handleCropEnd();
                } else if (isDragging) {
                    isDragging = false;
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
                break;
            case RECTANGLE:
                currentDrawingObject = new DrawingRectangle(bitmapX, bitmapY, brushColor, brushSize);
                break;
            case CIRCLE:
                currentDrawingObject = new DrawingCircle(bitmapX, bitmapY, brushColor, brushSize);
                break;
            case TEXT:
                if (!drawingText.isEmpty()) {
                    addTextAtPosition(bitmapX, bitmapY);
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
            undoStack.push(currentDrawingObject);
            redoStack.clear();
            currentDrawingObject = null;
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
        DrawingText text = new DrawingText(x, y, drawingText, fontFamily, textStyle, textSize, brushColor);
        drawingObjects.add(text);
        undoStack.push(text);
        redoStack.clear();
    }

    // Public methods for EditorActivity to use

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
                Bitmap croppedBitmap = Bitmap.createBitmap(workingBitmap, x, y, width, height);
                workingBitmap.recycle();
                workingBitmap = croppedBitmap;

                // Adjust drawing objects coordinates
                List<DrawingObject> objectsToKeep = new ArrayList<>();
                for (DrawingObject obj : drawingObjects) {
                    float newStartX = obj.getStartX() - x;
                    float newStartY = obj.getStartY() - y;
                    float newEndX = obj.getEndX() - x;
                    float newEndY = obj.getEndY() - y;

                    // Check if object is at least partially in the new image bounds
                    boolean isVisible = !(newEndX < 0 || newStartX > width || newEndY < 0 || newStartY > height);
                    if (isVisible) {
                        obj.updateStartPoint(newStartX, newStartY);
                        obj.updateEndPoint(newEndX, newEndY);
                        objectsToKeep.add(obj);
                    }
                }

                // Replace the objects list with only the visible ones
                drawingObjects.clear();
                drawingObjects.addAll(objectsToKeep);

                // Clear and rebuild undo stack
                undoStack.clear();
                for (DrawingObject obj : objectsToKeep) {
                    undoStack.push(obj);
                }
                redoStack.clear();

                // Reset transformations and fit the cropped image to view
                imageMatrix.reset();
                fitImageToView();

                cropMode = false;
                cropRect = null;
                invalidate();
            }
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            originalBitmap = bitmap;
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageMatrix.reset();
            fitImageToView();
            invalidate();
        }
    }

    public void fitImageToView() {
        if (workingBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        imageMatrix.reset();

        // Calculate scaling to fit the image without zooming in
        float scaleX = (float) getWidth() / workingBitmap.getWidth();
        float scaleY = (float) getHeight() / workingBitmap.getHeight();
        float scale = Math.min(scaleX, scaleY);

        // If scale > 1, it means we'd zoom in - avoid that by setting scale to 1
        if (scale > 1.0f) {
            scale = 1.0f;
        }

        imageMatrix.setScale(scale, scale);

        // Center the image in the view
        float dx = (getWidth() - workingBitmap.getWidth() * scale) / 2f;
        float dy = (getHeight() - workingBitmap.getHeight() * scale) / 2f;
        imageMatrix.postTranslate(dx, dy);

        invalidate();
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
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
    }

    public void setTextDrawingProperties(String text, String fontFamily, int style, int textSize, int color) {
        this.drawingText = text;
        this.fontFamily = fontFamily;
        this.textStyle = style;
        this.textSize = textSize;
        this.brushColor = color;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            DrawingObject lastDrawn = undoStack.pop();
            redoStack.push(lastDrawn);
            drawingObjects.remove(lastDrawn);
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            DrawingObject redoObj = redoStack.pop();
            undoStack.push(redoObj);
            drawingObjects.add(redoObj);
            invalidate();
        }
    }

    public Bitmap getFinalBitmap() {
        if (workingBitmap == null) return null;

        Bitmap resultBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(),
                workingBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resultBitmap);

        // Draw the original image first
        canvas.drawBitmap(workingBitmap, 0, 0, null);

        // Draw all objects directly in bitmap coordinates
        for (DrawingObject obj : drawingObjects) {
            obj.draw(canvas);
        }

        return resultBitmap;
    }
}