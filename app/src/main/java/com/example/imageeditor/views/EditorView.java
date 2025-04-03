package com.example.imageeditor.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorView extends View {

    // Режимы редактирования
    public static final int MODE_PAN = 0;
    public static final int MODE_DRAW = 1;
    public static final int MODE_RECT = 2;
    public static final int MODE_CIRCLE = 3;
    public static final int MODE_TEXT = 4;

    // Текущий режим редактирования
    private int currentMode = MODE_PAN;

    // Исходное изображение и его копия для редактирования
    private Bitmap originalImage;
    private Bitmap workingImage;

    // Матрица для трансформаций изображения
    private Matrix imageMatrix;

    // Параметры рисования
    private Paint paint;
    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 5f;

    // Для рисования линий
    private Path currentPath;

    // Для рисования фигур
    private float startX, startY, endX, endY;

    // Для добавления текста
    private String currentText = "";
    private float textX, textY;
    private Typeface currentTypeface = Typeface.DEFAULT;
    private boolean isBold = false;
    private boolean isItalic = false;

    // Списки нарисованных объектов
    private List<DrawObject> drawObjects = new ArrayList<>();

    // Стеки для отмены/повтора действий
    private Stack<DrawCommand> undoStack = new Stack<>();
    private Stack<DrawCommand> redoStack = new Stack<>();

    // Конструкторы
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

    // Инициализация компонентов
    private void init() {
        imageMatrix = new Matrix();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(currentStrokeWidth);
    }

    // Установка изображения
    public void setImage(Bitmap bitmap) {
        originalImage = bitmap;
        workingImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        resetMatrix();
        invalidate();
    }

    // Сброс матрицы трансформации
    private void resetMatrix() {
        imageMatrix.reset();
        if (originalImage != null) {
            float scaleX = (float) getWidth() / originalImage.getWidth();
            float scaleY = (float) getHeight() / originalImage.getHeight();
            float scale = Math.min(scaleX, scaleY);

            float dx = (getWidth() - originalImage.getWidth() * scale) / 2;
            float dy = (getHeight() - originalImage.getHeight() * scale) / 2;

            imageMatrix.setScale(scale, scale);
            imageMatrix.postTranslate(dx, dy);
        }
    }

    // Получение текущего изображения
    public Bitmap getEditedImage() {
        if (workingImage == null) return null;

        Bitmap result = Bitmap.createBitmap(workingImage.getWidth(), workingImage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(workingImage, 0, 0, null);

        // Отрисовка всех объектов на результирующем изображении
        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);

        for (DrawObject obj : drawObjects) {
            obj.draw(canvas, inverseMatrix);
        }

        return result;
    }

    // Установка режима редактирования
    public void setMode(int mode) {
        currentMode = mode;
    }

    // Установка цвета
    public void setColor(int color) {
        currentColor = color;
        paint.setColor(color);
    }

    // Установка толщины линии
    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }

    // Установка текста
    public void setText(String text) {
        currentText = text;
    }

    // Установка шрифта
    public void setTypeface(Typeface typeface) {
        currentTypeface = typeface;
    }

    // Установка стиля текста
    public void setTextStyle(boolean bold, boolean italic) {
        isBold = bold;
        isItalic = italic;

        int style = Typeface.NORMAL;
        if (bold && italic) {
            style = Typeface.BOLD_ITALIC;
        } else if (bold) {
            style = Typeface.BOLD;
        } else if (italic) {
            style = Typeface.ITALIC;
        }

        paint.setTypeface(Typeface.create(currentTypeface, style));
    }

    // Поворот изображения
    public void rotateImage(float degrees) {
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(degrees, workingImage.getWidth() / 2f, workingImage.getHeight() / 2f);

        Bitmap rotated = Bitmap.createBitmap(
                workingImage, 0, 0, workingImage.getWidth(), workingImage.getHeight(),
                rotateMatrix, true
        );

        workingImage = rotated;
        resetMatrix();
        invalidate();

        // Добавление в стек отмены
        undoStack.push(new RotateCommand(degrees));
        redoStack.clear();
    }

    // Отзеркаливание изображения
    public void flipImage(boolean horizontal) {
        Matrix flipMatrix = new Matrix();

        if (horizontal) {
            flipMatrix.setScale(-1, 1);
            flipMatrix.postTranslate(workingImage.getWidth(), 0);
        } else {
            flipMatrix.setScale(1, -1);
            flipMatrix.postTranslate(0, workingImage.getHeight());
        }

        Bitmap flipped = Bitmap.createBitmap(
                workingImage, 0, 0, workingImage.getWidth(), workingImage.getHeight(),
                flipMatrix, true
        );

        workingImage = flipped;
        resetMatrix();
        invalidate();

        // Добавление в стек отмены
        undoStack.push(new FlipCommand(horizontal));
        redoStack.clear();
    }

    // Обрезка изображения
    public void cropImage(RectF cropRect) {
        Matrix inverseMatrix = new Matrix();
        imageMatrix.invert(inverseMatrix);

        RectF mappedRect = new RectF();
        inverseMatrix.mapRect(mappedRect, cropRect);

        int x = (int) mappedRect.left;
        int y = (int) mappedRect.top;
        int width = (int) mappedRect.width();
        int height = (int) mappedRect.height();

        // Проверка границ
        x = Math.max(0, x);
        y = Math.max(0, y);
        width = Math.min(width, workingImage.getWidth() - x);
        height = Math.min(height, workingImage.getHeight() - y);

        if (width <= 0 || height <= 0) return;

        Bitmap cropped = Bitmap.createBitmap(workingImage, x, y, width, height);
        workingImage = cropped;
        resetMatrix();
        invalidate();

        // Добавление в стек отмены
        undoStack.push(new CropCommand(new RectF(x, y, x + width, y + height)));
        redoStack.clear();
    }

    // Отмена последнего действия
    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        DrawCommand command = undoStack.pop();
        redoStack.push(command);

        if (command instanceof AddObjectCommand) {
            int index = ((AddObjectCommand) command).getObjectIndex();
            if (index >= 0 && index < drawObjects.size()) {
                drawObjects.remove(index);
            }
        } else {
            // Для операций с изображением нужно восстановить предыдущее состояние
            // В реальном приложении здесь должно быть сохранение состояний
        }

        invalidate();
        return true;
    }

    // Повтор последнего отмененного действия
    public boolean redo() {
        if (redoStack.isEmpty()) return false;

        DrawCommand command = redoStack.pop();
        undoStack.push(command);

        if (command instanceof AddObjectCommand) {
            DrawObject obj = ((AddObjectCommand) command).getObject();
            if (obj != null) {
                drawObjects.add(obj);
            }
        } else {
            // Для операций с изображением нужно восстановить следующее состояние
            // В реальном приложении здесь должно быть восстановление состояний
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Отрисовка фона
        canvas.drawColor(Color.LTGRAY);

        // Отрисовка изображения
        if (workingImage != null) {
            canvas.drawBitmap(workingImage, imageMatrix, null);
        }

        // Отрисовка всех объектов
        for (DrawObject obj : drawObjects) {
            obj.draw(canvas, null);
        }

        // Отрисовка текущего объекта (при рисовании)
        if (currentMode == MODE_DRAW && currentPath != null) {
            canvas.drawPath(currentPath, paint);
        } else if ((currentMode == MODE_RECT || currentMode == MODE_CIRCLE) && startX != endX && startY != endY) {
            Paint tempPaint = new Paint(paint);

            if (currentMode == MODE_RECT) {
                canvas.drawRect(startX, startY, endX, endY, tempPaint);
            } else {
                float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                float centerX = (startX + endX) / 2;
                float centerY = (startY + endY) / 2;
                canvas.drawCircle(centerX, centerY, radius, tempPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                handleTouchEnd(x, y);
                break;
        }

        invalidate();
        return true;
    }

    private void handleTouchStart(float x, float y) {
        switch (currentMode) {
            case MODE_DRAW:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                break;
            case MODE_RECT:
            case MODE_CIRCLE:
                startX = x;
                startY = y;
                endX = x;
                endY = y;
                break;
            case MODE_TEXT:
                textX = x;
                textY = y;
                // Здесь должен быть вызов диалога для ввода текста
                break;
        }
    }

    private void handleTouchMove(float x, float y) {
        switch (currentMode) {
            case MODE_DRAW:
                currentPath.lineTo(x, y);
                break;
            case MODE_RECT:
            case MODE_CIRCLE:
                endX = x;
                endY = y;
                break;
        }
    }

    private void handleTouchEnd(float x, float y) {
        switch (currentMode) {
            case MODE_DRAW:
                if (currentPath != null) {
                    DrawObject pathObj = new PathObject(
                            new Path(currentPath), new Paint(paint)
                    );
                    drawObjects.add(pathObj);
                    undoStack.push(new AddObjectCommand(pathObj, drawObjects.size() - 1));
                    redoStack.clear();
                    currentPath = null;
                }
                break;
            case MODE_RECT:
                DrawObject rectObj = new RectObject(
                        startX, startY, endX, endY, new Paint(paint)
                );
                drawObjects.add(rectObj);
                undoStack.push(new AddObjectCommand(rectObj, drawObjects.size() - 1));
                redoStack.clear();
                break;
            case MODE_CIRCLE:
                float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                float centerX = (startX + endX) / 2;
                float centerY = (startY + endY) / 2;

                DrawObject circleObj = new CircleObject(
                        centerX, centerY, radius, new Paint(paint)
                );
                drawObjects.add(circleObj);
                undoStack.push(new AddObjectCommand(circleObj, drawObjects.size() - 1));
                redoStack.clear();
                break;
            case MODE_TEXT:
                if (!currentText.isEmpty()) {
                    DrawObject textObj = new TextObject(
                            currentText, textX, textY, new Paint(paint)
                    );
                    drawObjects.add(textObj);
                    undoStack.push(new AddObjectCommand(textObj, drawObjects.size() - 1));
                    redoStack.clear();
                }
                break;
        }
    }

    // Вспомогательные классы для нарисованных объектов

    // Базовый класс для всех объектов рисования
    abstract class DrawObject {
        Paint paint;

        DrawObject(Paint paint) {
            this.paint = new Paint(paint);
        }

        abstract void draw(Canvas canvas, Matrix transformMatrix);
    }

    // Класс для линий (путей)
    class PathObject extends DrawObject {
        Path path;

        PathObject(Path path, Paint paint) {
            super(paint);
            this.path = new Path(path);
        }

        @Override
        void draw(Canvas canvas, Matrix transformMatrix) {
            if (transformMatrix != null) {
                Path transformedPath = new Path();
                path.transform(transformMatrix, transformedPath);
                canvas.drawPath(transformedPath, paint);
            } else {
                canvas.drawPath(path, paint);
            }
        }
    }

    // Класс для прямоугольников
    class RectObject extends DrawObject {
        float left, top, right, bottom;

        RectObject(float left, float top, float right, float bottom, Paint paint) {
            super(paint);
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        @Override
        void draw(Canvas canvas, Matrix transformMatrix) {
            if (transformMatrix != null) {
                RectF rect = new RectF(left, top, right, bottom);
                RectF transformedRect = new RectF();
                transformMatrix.mapRect(transformedRect, rect);
                canvas.drawRect(transformedRect, paint);
            } else {
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }

    // Класс для кругов
    class CircleObject extends DrawObject {
        float centerX, centerY, radius;

        CircleObject(float centerX, float centerY, float radius, Paint paint) {
            super(paint);
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
        }

        @Override
        void draw(Canvas canvas, Matrix transformMatrix) {
            if (transformMatrix != null) {
                float[] points = new float[] {centerX, centerY};
                transformMatrix.mapPoints(points);

                // Для упрощения просто применяем масштаб к радиусу
                float[] values = new float[9];
                transformMatrix.getValues(values);
                float scaleX = values[Matrix.MSCALE_X];
                float scaleY = values[Matrix.MSCALE_Y];
                float scaledRadius = radius * Math.max(scaleX, scaleY);

                canvas.drawCircle(points[0], points[1], scaledRadius, paint);
            } else {
                canvas.drawCircle(centerX, centerY, radius, paint);
            }
        }
    }

    // Класс для текста
    class TextObject extends DrawObject {
        String text;
        float x, y;

        TextObject(String text, float x, float y, Paint paint) {
            super(paint);
            this.text = text;
            this.x = x;
            this.y = y;
            this.paint.setStyle(Paint.Style.FILL);
        }

        @Override
        void draw(Canvas canvas, Matrix transformMatrix) {
            if (transformMatrix != null) {
                float[] points = new float[] {x, y};
                transformMatrix.mapPoints(points);
                canvas.drawText(text, points[0], points[1], paint);
            } else {
                canvas.drawText(text, x, y, paint);
            }
        }
    }

    // Интерфейс для команд отмены/повтора
    interface DrawCommand {}

    // Класс для добавления объекта (для отмены/повтора)
    class AddObjectCommand implements DrawCommand {
        private DrawObject object;
        private int objectIndex;

        AddObjectCommand(DrawObject object, int index) {
            this.object = object;
            this.objectIndex = index;
        }

        DrawObject getObject() {
            return object;
        }

        int getObjectIndex() {
            return objectIndex;
        }
    }

    // Класс для поворота изображения (для отмены/повтора)
    class RotateCommand implements DrawCommand {
        private float degrees;

        RotateCommand(float degrees) {
            this.degrees = degrees;
        }
    }

    // Класс для отзеркаливания изображения (для отмены/повтора)
    class FlipCommand implements DrawCommand {
        private boolean horizontal;

        FlipCommand(boolean horizontal) {
            this.horizontal = horizontal;
        }
    }

    // Класс для обрезки изображения (для отмены/повтора)
    class CropCommand implements DrawCommand {
        private RectF cropRect;

        CropCommand(RectF cropRect) {
            this.cropRect = cropRect;
        }
    }
}