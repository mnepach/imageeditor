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
    private int cropHandleRadius = 40;
    private int selectedCropHandle = -1;

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
            cropPaint.setStrokeWidth(2f);
            canvas.drawRect(cropRect, cropPaint);

            // Рисуем маркеры углов обрезки
            cropPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cropRect.left, cropRect.top, cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.right, cropRect.top, cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.left, cropRect.bottom, cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.right, cropRect.bottom, cropHandleRadius, cropPaint);

            // Добавляем маркеры для сторон
            canvas.drawCircle(cropRect.centerX(), cropRect.top, cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.centerX(), cropRect.bottom, cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.left, cropRect.centerY(), cropHandleRadius, cropPaint);
            canvas.drawCircle(cropRect.right, cropRect.centerY(), cropHandleRadius, cropPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // Обновляем обратную матрицу трансформации
        updateInverseMatrix();

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
                Log.d(TAG, "Касание: (" + bitmapX + ", " + bitmapY + ")");
                if (cropMode) {
                    return handleCropStart(x, y);
                } else {
                    handleDrawStart(bitmapX, bitmapY);
                    invalidate();
                    return true;
                }
            case MotionEvent.ACTION_MOVE:
                if (cropMode) {
                    return handleCropMove(x, y);
                } else {
                    handleDrawMove(bitmapX, bitmapY);
                    invalidate();
                    return true;
                }
            case MotionEvent.ACTION_UP:
                if (cropMode) {
                    return handleCropEnd();
                } else {
                    handleDrawEnd();
                    invalidate();
                    return true;
                }
            default:
                return super.onTouchEvent(event);
        }
    }

    // Обновление обратной матрицы трансформации
    private void updateInverseMatrix() {
        imageMatrix.invert(inverseMatrix);
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
            // Constrain coordinates to bitmap boundaries
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
            Log.d(TAG, "Объект добавлен в историю: " + currentDrawingObject.getClass().getSimpleName());
            currentDrawingObject = null;
        }
    }

    private boolean handleCropStart(float x, float y) {
        if (workingBitmap == null) return false;

        if (cropRect != null) {
            // Проверяем, не потянули ли мы за маркер обрезки
            selectedCropHandle = getCropHandleUnderPoint(x, y);
            if (selectedCropHandle >= 0) {
                Log.d(TAG, "Выбран маркер обрезки: " + selectedCropHandle);
                invalidate();
                return true;
            }
        }

        // Если не попали в маркер, создаем новую область обрезки
        // Ограничиваем начальные координаты границами изображения
        PointF imagePoint = mapPointToImageSpace(x, y);
        cropRect = new RectF(imagePoint.x, imagePoint.y, imagePoint.x, imagePoint.y);
        selectedCropHandle = 4; // Правый нижний угол по умолчанию
        invalidate();
        Log.d(TAG, "Начата обрезка: (" + imagePoint.x + ", " + imagePoint.y + ")");
        return true;
    }

    private boolean handleCropMove(float x, float y) {
        if (cropRect == null || workingBitmap == null || selectedCropHandle < 0) return false;

        PointF imagePoint = mapPointToImageSpace(x, y);

        // Обновляем положение выбранного маркера
        switch (selectedCropHandle) {
            case 0: // Левый верхний
                cropRect.left = Math.max(imageBounds.left, Math.min(imagePoint.x, cropRect.right - 50));
                cropRect.top = Math.max(imageBounds.top, Math.min(imagePoint.y, cropRect.bottom - 50));
                break;
            case 1: // Правый верхний
                cropRect.right = Math.min(imageBounds.right, Math.max(imagePoint.x, cropRect.left + 50));
                cropRect.top = Math.max(imageBounds.top, Math.min(imagePoint.y, cropRect.bottom - 50));
                break;
            case 2: // Левый нижний
                cropRect.left = Math.max(imageBounds.left, Math.min(imagePoint.x, cropRect.right - 50));
                cropRect.bottom = Math.min(imageBounds.bottom, Math.max(imagePoint.y, cropRect.top + 50));
                break;
            case 3: // Правый нижний
                cropRect.right = Math.min(imageBounds.right, Math.max(imagePoint.x, cropRect.left + 50));
                cropRect.bottom = Math.min(imageBounds.bottom, Math.max(imagePoint.y, cropRect.top + 50));
                break;
            case 4: // Верх центр
                cropRect.top = Math.max(imageBounds.top, Math.min(imagePoint.y, cropRect.bottom - 50));
                break;
            case 5: // Низ центр
                cropRect.bottom = Math.min(imageBounds.bottom, Math.max(imagePoint.y, cropRect.top + 50));
                break;
            case 6: // Левый центр
                cropRect.left = Math.max(imageBounds.left, Math.min(imagePoint.x, cropRect.right - 50));
                break;
            case 7: // Правый центр
                cropRect.right = Math.min(imageBounds.right, Math.max(imagePoint.x, cropRect.left + 50));
                break;
        }

        invalidate();
        return true;
    }

    private boolean handleCropEnd() {
        if (cropRect != null) {
            // Нормализуем прямоугольник (убеждаемся, что left < right и top < bottom)
            float left = Math.min(cropRect.left, cropRect.right);
            float top = Math.min(cropRect.top, cropRect.bottom);
            float right = Math.max(cropRect.left, cropRect.right);
            float bottom = Math.max(cropRect.top, cropRect.bottom);
            cropRect.set(left, top, right, bottom);
            selectedCropHandle = -1;
            invalidate();
            Log.d(TAG, "Обрезка завершена: " + cropRect.toString());
            return true;
        }
        return false;
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

    // Преобразование координат из экранного пространства в пространство изображения
    private PointF mapPointToImageSpace(float x, float y) {
        // Ограничиваем точку границами изображения на экране
        x = Math.max(imageBounds.left, Math.min(x, imageBounds.right));
        y = Math.max(imageBounds.top, Math.min(y, imageBounds.bottom));

        // Преобразуем экранные координаты в координаты изображения
        float[] points = {x, y};
        inverseMatrix.mapPoints(points);

        // Ограничиваем координаты внутри битмапа
        float bitmapX = Math.max(0, Math.min(points[0], workingBitmap.getWidth()));
        float bitmapY = Math.max(0, Math.min(points[1], workingBitmap.getHeight()));

        return new PointF(bitmapX, bitmapY);
    }

    public void applyCrop() {
        if (cropMode && cropRect != null && workingBitmap != null) {
            // Переводим координаты cropRect из экранного пространства в пространство изображения
            RectF cropRectInBitmap = new RectF();
            Matrix inverseMatrix = new Matrix();
            imageMatrix.invert(inverseMatrix);

            // Создаем прямоугольник в пространстве изображения
            cropRectInBitmap.set(cropRect);
            inverseMatrix.mapRect(cropRectInBitmap);

            // Ограничиваем координаты внутри изображения
            int x = Math.max(0, Math.round(cropRectInBitmap.left));
            int y = Math.max(0, Math.round(cropRectInBitmap.top));
            int width = Math.min(workingBitmap.getWidth() - x, Math.round(cropRectInBitmap.width()));
            int height = Math.min(workingBitmap.getHeight() - y, Math.round(cropRectInBitmap.height()));

            if (width > 0 && height > 0) {
                // Создаем новый Canvas и рисуем все объекты на текущую битмапу
                Canvas canvas = new Canvas(workingBitmap);
                for (DrawingObject obj : drawingObjects) {
                    obj.draw(canvas);
                }

                // Создаем обрезанную битмапу
                Bitmap croppedBitmap = Bitmap.createBitmap(workingBitmap, x, y, width, height);
                workingBitmap = croppedBitmap;

                // Создаем новый canvas для рисования
                bitmapCanvas = new Canvas(workingBitmap);

                // Очищаем списки объектов и историю
                drawingObjects.clear();
                historyManager.clear();

                // Сбрасываем матрицу и подгоняем изображение к экрану
                imageMatrix.reset();
                fitImageToView();

                // Выключаем режим обрезки
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
            Log.d(TAG, "Изображение повернуто на " + degrees + " градусов");
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
            Log.d(TAG, "Изображение отражено по горизонтали");
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

            Log.d(TAG, "Создано финальное изображение");
            return resultBitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Ошибка при создании финального изображения: не хватает памяти", e);
            return null;
        }
    }
}