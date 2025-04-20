package com.example.imageeditor.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
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

    private Canvas bitmapCanvas;
    private Bitmap originalBitmap;
    private Bitmap workingBitmap;
    private Matrix imageMatrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();
    private RectF imageBounds = new RectF();

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
    private int cropHandleRadius = 30;
    private int selectedCropHandle = -1;
    private float lastTouchX, lastTouchY;
    private boolean isDraggingCropArea = false;

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
            // Обновляем границы изображения
            imageBounds.set(0, 0, workingBitmap.getWidth(), workingBitmap.getHeight());
            imageMatrix.mapRect(imageBounds);
        }

        // Рисуем все объекты с учетом текущей матрицы трансформации
        for (DrawingObject obj : drawingObjects) {
            canvas.save();
            canvas.concat(imageMatrix);
            obj.draw(canvas);
            canvas.restore();
        }

        // Рисуем текущий объект рисования
        if (currentDrawingObject != null) {
            canvas.save();
            canvas.concat(imageMatrix);
            currentDrawingObject.draw(canvas);
            canvas.restore();
        }

        // Рисуем интерфейс обрезки, если активен режим обрезки
        if (cropMode && cropRect != null) {
            Paint cropPaint = new Paint();
            cropPaint.setColor(0xFFFFFFFF);
            cropPaint.setStyle(Paint.Style.STROKE);
            cropPaint.setStrokeWidth(3f);

            // Внешняя область затемнения
            Paint dimPaint = new Paint();
            dimPaint.setColor(0x88000000);
            dimPaint.setStyle(Paint.Style.FILL);

            // Рисуем затемнение вокруг области обрезки
            // Верхняя область
            canvas.drawRect(imageBounds.left, imageBounds.top, imageBounds.right, cropRect.top, dimPaint);
            // Левая область
            canvas.drawRect(imageBounds.left, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
            // Правая область
            canvas.drawRect(cropRect.right, cropRect.top, imageBounds.right, cropRect.bottom, dimPaint);
            // Нижняя область
            canvas.drawRect(imageBounds.left, cropRect.bottom, imageBounds.right, imageBounds.bottom, dimPaint);

            // Рамка области обрезки
            canvas.drawRect(cropRect, cropPaint);

            // Рисуем маркеры углов и сторон
            cropPaint.setStyle(Paint.Style.FILL);
            cropPaint.setColor(0xFFFFFFFF);
            cropPaint.setStrokeWidth(2f);

            // Угловые маркеры
            drawCropHandle(canvas, cropRect.left, cropRect.top, cropPaint);      // Левый верхний
            drawCropHandle(canvas, cropRect.right, cropRect.top, cropPaint);     // Правый верхний
            drawCropHandle(canvas, cropRect.left, cropRect.bottom, cropPaint);   // Левый нижний
            drawCropHandle(canvas, cropRect.right, cropRect.bottom, cropPaint);  // Правый нижний

            // Маркеры сторон
            drawCropHandle(canvas, cropRect.centerX(), cropRect.top, cropPaint);      // Верхний центр
            drawCropHandle(canvas, cropRect.centerX(), cropRect.bottom, cropPaint);   // Нижний центр
            drawCropHandle(canvas, cropRect.left, cropRect.centerY(), cropPaint);     // Левый центр
            drawCropHandle(canvas, cropRect.right, cropRect.centerY(), cropPaint);    // Правый центр
        }
    }

    private void drawCropHandle(Canvas canvas, float x, float y, Paint paint) {
        canvas.drawCircle(x, y, cropHandleRadius, paint);
        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(0xFF000000);
        strokePaint.setStrokeWidth(2f);
        canvas.drawCircle(x, y, cropHandleRadius, strokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // Обновляем обратную матрицу трансформации
        updateInverseMatrix();

        if (cropMode) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = x;
                    lastTouchY = y;
                    if (cropRect == null) {
                        // Если нет прямоугольника обрезки, создаем новый
                        initializeCropRect(x, y);
                        return true;
                    } else {
                        // Проверяем, нажат ли один из маркеров или внутри области
                        selectedCropHandle = getCropHandleUnderPoint(x, y);
                        if (selectedCropHandle >= 0) {
                            // Нажат маркер
                            return true;
                        } else if (isTouchInsideCropRect(x, y)) {
                            // Нажатие внутри области обрезки - перемещаем всю область
                            isDraggingCropArea = true;
                            return true;
                        }
                        // Создаем новую область обрезки
                        initializeCropRect(x, y);
                        return true;
                    }

                case MotionEvent.ACTION_MOVE:
                    if (selectedCropHandle >= 0) {
                        // Перемещаем маркер
                        moveCropHandle(selectedCropHandle, x, y);
                        invalidate();
                        return true;
                    } else if (isDraggingCropArea) {
                        // Перемещаем всю область обрезки
                        moveCropArea(x - lastTouchX, y - lastTouchY);
                        lastTouchX = x;
                        lastTouchY = y;
                        invalidate();
                        return true;
                    } else if (cropRect != null) {
                        // Изменяем размер области обрезки с начальной точки
                        updateCropRectSize(x, y);
                        invalidate();
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Завершаем операцию
                    selectedCropHandle = -1;
                    isDraggingCropArea = false;
                    normalizeCropRect();
                    invalidate();
                    return true;
            }
        } else {
            // Преобразуем координаты касания в координаты на изображении
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
                    handleDrawStart(bitmapX, bitmapY);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    handleDrawMove(bitmapX, bitmapY);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    handleDrawEnd();
                    invalidate();
                    return true;
            }
        }

        return super.onTouchEvent(event);
    }

    private boolean isTouchInsideCropRect(float x, float y) {
        if (cropRect == null) return false;
        return cropRect.contains(x, y);
    }

    private void initializeCropRect(float x, float y) {
        // Создаем начальную точку для прямоугольника обрезки
        cropRect = new RectF(x, y, x, y);
        selectedCropHandle = -1;
        isDraggingCropArea = false;
    }

    private void updateCropRectSize(float x, float y) {
        if (cropRect != null) {
            // Ограничиваем координаты в пределах изображения
            x = Math.max(imageBounds.left, Math.min(x, imageBounds.right));
            y = Math.max(imageBounds.top, Math.min(y, imageBounds.bottom));

            cropRect.right = x;
            cropRect.bottom = y;
        }
    }

    private void moveCropArea(float dx, float dy) {
        if (cropRect != null) {
            // Проверяем, чтобы область не выходила за границы изображения
            float newLeft = Math.max(imageBounds.left, Math.min(cropRect.left + dx, imageBounds.right - cropRect.width()));
            float newTop = Math.max(imageBounds.top, Math.min(cropRect.top + dy, imageBounds.bottom - cropRect.height()));

            float widthDiff = newLeft - cropRect.left;
            float heightDiff = newTop - cropRect.top;

            cropRect.offset(widthDiff, heightDiff);
        }
    }

    private void moveCropHandle(int handleIndex, float x, float y) {
        if (cropRect == null) return;

        // Ограничиваем координаты в пределах изображения
        x = Math.max(imageBounds.left, Math.min(x, imageBounds.right));
        y = Math.max(imageBounds.top, Math.min(y, imageBounds.bottom));

        // Минимальный размер области обрезки
        float minSize = cropHandleRadius * 3;

        switch (handleIndex) {
            case 0: // Левый верхний
                cropRect.left = Math.min(cropRect.right - minSize, x);
                cropRect.top = Math.min(cropRect.bottom - minSize, y);
                break;
            case 1: // Правый верхний
                cropRect.right = Math.max(cropRect.left + minSize, x);
                cropRect.top = Math.min(cropRect.bottom - minSize, y);
                break;
            case 2: // Левый нижний
                cropRect.left = Math.min(cropRect.right - minSize, x);
                cropRect.bottom = Math.max(cropRect.top + minSize, y);
                break;
            case 3: // Правый нижний
                cropRect.right = Math.max(cropRect.left + minSize, x);
                cropRect.bottom = Math.max(cropRect.top + minSize, y);
                break;
            case 4: // Верхний центр
                cropRect.top = Math.min(cropRect.bottom - minSize, y);
                break;
            case 5: // Нижний центр
                cropRect.bottom = Math.max(cropRect.top + minSize, y);
                break;
            case 6: // Левый центр
                cropRect.left = Math.min(cropRect.right - minSize, x);
                break;
            case 7: // Правый центр
                cropRect.right = Math.max(cropRect.left + minSize, x);
                break;
        }
    }

    private void normalizeCropRect() {
        if (cropRect != null) {
            // Убеждаемся, что left < right и top < bottom
            float left = Math.min(cropRect.left, cropRect.right);
            float top = Math.min(cropRect.top, cropRect.bottom);
            float right = Math.max(cropRect.left, cropRect.right);
            float bottom = Math.max(cropRect.top, cropRect.bottom);

            // Минимальный размер для области обрезки
            float minSize = cropHandleRadius * 3;
            if (right - left < minSize) right = left + minSize;
            if (bottom - top < minSize) bottom = top + minSize;

            cropRect.set(left, top, right, bottom);
        }
    }

    // Обновление обратной матрицы трансформации
    private void updateInverseMatrix() {
        imageMatrix.invert(inverseMatrix);
    }

    // Определение, какой маркер обрезки находится под указанной точкой
    private int getCropHandleUnderPoint(float x, float y) {
        if (cropRect == null) return -1;

        // Проверяем расстояние от точки до каждого маркера
        float[] handlePoints = {
                cropRect.left, cropRect.top,           // 0: левый верхний
                cropRect.right, cropRect.top,          // 1: правый верхний
                cropRect.left, cropRect.bottom,        // 2: левый нижний
                cropRect.right, cropRect.bottom,       // 3: правый нижний
                cropRect.centerX(), cropRect.top,      // 4: верх центр
                cropRect.centerX(), cropRect.bottom,   // 5: низ центр
                cropRect.left, cropRect.centerY(),     // 6: левый центр
                cropRect.right, cropRect.centerY()     // 7: правый центр
        };

        for (int i = 0; i < 8; i++) {
            float handleX = handlePoints[i * 2];
            float handleY = handlePoints[i * 2 + 1];
            float distance = (float) Math.sqrt(Math.pow(x - handleX, 2) + Math.pow(y - handleY, 2));
            if (distance <= cropHandleRadius * 1.5) {
                return i;
            }
        }

        return -1;
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
                    currentDrawingObject = new DrawingText(bitmapX, bitmapY, drawingText, fontFamily, textStyle, textSize, brushColor);
                }
                break;
        }
    }

    private void handleDrawMove(float bitmapX, float bitmapY) {
        if (currentDrawingObject != null) {
            // Ограничиваем координаты внутри битмапы
            if (workingBitmap != null) {
                bitmapX = Math.max(0, Math.min(bitmapX, workingBitmap.getWidth()));
                bitmapY = Math.max(0, Math.min(bitmapY, workingBitmap.getHeight()));
            }

            if (currentDrawingObject instanceof DrawingLine) {
                ((DrawingLine) currentDrawingObject).addPoint(bitmapX, bitmapY);
            } else {
                currentDrawingObject.updateEndPoint(bitmapX, bitmapY);
            }
        }
    }

    private void handleDrawEnd() {
        if (currentDrawingObject != null) {
            constrainToImageBounds(currentDrawingObject);
            drawingObjects.add(currentDrawingObject);
            historyManager.executeCommand(new DrawCommand(drawingObjects, currentDrawingObject));
            currentDrawingObject = null;
        }
    }

    // Проверка и обновление границ рисуемых объектов
    private void constrainToImageBounds(DrawingObject object) {
        if (workingBitmap == null) return;

        // Ограничение координат точек объекта внутри изображения
        float startX = Math.max(0, Math.min(object.getStartX(), workingBitmap.getWidth()));
        float startY = Math.max(0, Math.min(object.getStartY(), workingBitmap.getHeight()));
        float endX = Math.max(0, Math.min(object.getEndX(), workingBitmap.getWidth()));
        float endY = Math.max(0, Math.min(object.getEndY(), workingBitmap.getHeight()));

        object.updateStartPoint(startX, startY);
        object.updateEndPoint(endX, endY);
    }

    public void applyCrop() {
        if (cropMode && cropRect != null && workingBitmap != null) {
            // Преобразуем координаты cropRect из экранного пространства в пространство изображения
            RectF bitmapCropRect = new RectF();
            Matrix inverse = new Matrix();
            imageMatrix.invert(inverse);
            bitmapCropRect.set(cropRect);
            inverse.mapRect(bitmapCropRect);

            // Ограничиваем координаты внутри изображения
            int x = Math.max(0, Math.round(bitmapCropRect.left));
            int y = Math.max(0, Math.round(bitmapCropRect.top));
            int width = Math.min(workingBitmap.getWidth() - x, Math.round(bitmapCropRect.width()));
            int height = Math.min(workingBitmap.getHeight() - y, Math.round(bitmapCropRect.height()));

            if (width > 0 && height > 0) {
                // Сначала отрисовываем все объекты на рабочую битмапу
                Canvas canvas = new Canvas(workingBitmap);
                for (DrawingObject obj : drawingObjects) {
                    obj.draw(canvas);
                }

                try {
                    // Создаем обрезанную битмапу
                    Bitmap croppedBitmap = Bitmap.createBitmap(workingBitmap, x, y, width, height);

                    // Заменяем рабочую битмапу на обрезанную
                    workingBitmap = croppedBitmap;
                    bitmapCanvas = new Canvas(workingBitmap);

                    // Очищаем списки объектов и историю
                    drawingObjects.clear();
                    historyManager.clear();

                    // Сбрасываем матрицу и подгоняем изображение к экрану
                    imageMatrix.reset();
                    fitImageToView();

                    // Сбрасываем режим обрезки
                    cropMode = false;
                    cropRect = null;

                    invalidate();
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при обрезке изображения", e);
                }
            }
        }

        // В любом случае выключаем режим обрезки
        cropMode = false;
        cropRect = null;
        invalidate();
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            originalBitmap = bitmap;
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmapCanvas = new Canvas(workingBitmap);
            imageMatrix.reset();
            fitImageToView();
            invalidate();
        }
    }

    public void fitImageToView() {
        if (workingBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        imageMatrix.reset();
        float scaleX = (float) getWidth() / workingBitmap.getWidth();
        float scaleY = (float) getHeight() / workingBitmap.getHeight();
        float scale = Math.min(scaleX, scaleY);

        // Ограничиваем масштаб, чтобы изображение не увеличивалось больше 100%
        if (scale > 1.0f) {
            scale = 1.0f;
        }

        imageMatrix.setScale(scale, scale);
        float dx = (getWidth() - workingBitmap.getWidth() * scale) / 2f;
        float dy = (getHeight() - workingBitmap.getHeight() * scale) / 2f;
        imageMatrix.postTranslate(dx, dy);

        // Обновляем обратную матрицу
        updateInverseMatrix();

        // Обновляем границы изображения
        imageBounds.set(0, 0, workingBitmap.getWidth(), workingBitmap.getHeight());
        imageMatrix.mapRect(imageBounds);

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
        invalidate();
    }

    public boolean isCropModeActive() {
        return cropMode;
    }

    public void rotateImage(int degrees) {
        if (workingBitmap == null) return;

        // Применяем все текущие рисунки к изображению перед поворотом
        applyDrawingsToCanvas();

        // Создаем матрицу поворота с центром в середине изображения
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(degrees, workingBitmap.getWidth() / 2f, workingBitmap.getHeight() / 2f);

        try {
            // Создаем повернутую битмапу
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    workingBitmap, 0, 0, workingBitmap.getWidth(), workingBitmap.getHeight(), rotateMatrix, true);

            // Обновляем рабочую битмапу и канвас
            if (rotatedBitmap != workingBitmap) {
                workingBitmap.recycle();
                workingBitmap = rotatedBitmap;
                bitmapCanvas = new Canvas(workingBitmap);
            }

            // Очищаем список объектов рисования и историю
            drawingObjects.clear();
            historyManager.clear();

            // Обновляем отображение
            fitImageToView();
            invalidate();
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Ошибка при повороте изображения: не хватает памяти", e);
        }
    }

    public void flipImage() {
        if (workingBitmap == null) return;

        // Применяем все текущие рисунки к изображению перед отражением
        applyDrawingsToCanvas();

        // Создаем матрицу отражения
        Matrix flipMatrix = new Matrix();
        flipMatrix.setScale(-1, 1);
        flipMatrix.postTranslate(workingBitmap.getWidth(), 0);

        try {
            // Создаем отраженную битмапу
            Bitmap flippedBitmap = Bitmap.createBitmap(
                    workingBitmap, 0, 0, workingBitmap.getWidth(), workingBitmap.getHeight(), flipMatrix, true);

            // Обновляем рабочую битмапу и канвас
            if (flippedBitmap != workingBitmap) {
                workingBitmap.recycle();
                workingBitmap = flippedBitmap;
                bitmapCanvas = new Canvas(workingBitmap);
            }

            // Очищаем список объектов рисования и историю
            drawingObjects.clear();
            historyManager.clear();

            // Обновляем отображение
            fitImageToView();
            invalidate();
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Ошибка при отражении изображения: не хватает памяти", e);
        }
    }

    // Метод для применения всех текущих рисунков к канвасу
    private void applyDrawingsToCanvas() {
        if (workingBitmap != null && !drawingObjects.isEmpty()) {
            for (DrawingObject obj : drawingObjects) {
                obj.draw(bitmapCanvas);
            }
            drawingObjects.clear();
            historyManager.clear();
        }
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
        historyManager.undo();
        invalidate();
    }

    public void redo() {
        historyManager.redo();
        invalidate();
    }

    public Bitmap getFinalBitmap() {
        if (workingBitmap == null) return null;

        try {
            // Создаем копию текущего изображения
            Bitmap resultBitmap = Bitmap.createBitmap(
                    workingBitmap.getWidth(), workingBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultBitmap);

            // Рисуем текущее изображение
            canvas.drawBitmap(workingBitmap, 0, 0, null);

            // Рисуем все объекты поверх
            for (DrawingObject obj : drawingObjects) {
                obj.draw(canvas);
            }

            return resultBitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Ошибка при создании финального изображения: не хватает памяти", e);
            return null;
        }
    }
}