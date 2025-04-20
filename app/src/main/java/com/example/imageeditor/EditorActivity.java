package com.example.imageeditor;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imageeditor.utils.BitmapUtils;
import com.example.imageeditor.views.EditorView;
import com.example.imageeditor.views.ToolbarView;

import java.io.OutputStream;

public class EditorActivity extends AppCompatActivity {
    private static final String TAG = "EditorActivity";

    private EditorView editorView;
    private ToolbarView toolbarView;
    private LinearLayout settingsPanel;
    private LinearLayout brushSettings;
    private LinearLayout shapeSettings;
    private LinearLayout textSettings;

    private SeekBar seekBarBrushSize;
    private SeekBar seekBarTextSize;
    private Button btnColor;
    private Button btnShapeColor;
    private Button btnTextColor;
    private View colorIndicatorBrush;
    private View colorIndicatorShape;
    private View colorIndicatorText;
    private EditText editTextInput;
    private CheckBox checkBoxBold;
    private CheckBox checkBoxItalic;
    private Spinner spinnerFont;
    private Button btnConfirmCrop;
    private Button btnCancelCrop;

    private int currentColor = Color.BLACK;
    private int currentBrushSize = 5;
    private int currentTextSize = 40;
    private String currentFont = "sans-serif";
    private int currentTextStyle = Typeface.NORMAL;
    private String currentText = "";

    private enum EditorMode {
        NONE, LINE, RECTANGLE, CIRCLE, TEXT
    }

    private EditorMode currentMode = EditorMode.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Инициализация UI
        editorView = findViewById(R.id.editorView);
        toolbarView = findViewById(R.id.toolbarView);
        settingsPanel = findViewById(R.id.settingsPanel);
        brushSettings = findViewById(R.id.brushSettings);
        shapeSettings = findViewById(R.id.shapeSettings);
        textSettings = findViewById(R.id.textSettings);
        seekBarBrushSize = findViewById(R.id.seekBarBrushSize);
        seekBarTextSize = findViewById(R.id.seekBarTextSize);
        btnColor = findViewById(R.id.btnColor);
        btnShapeColor = findViewById(R.id.btnShapeColor);
        btnTextColor = findViewById(R.id.btnTextColor);
        colorIndicatorBrush = findViewById(R.id.colorIndicatorBrush);
        colorIndicatorShape = findViewById(R.id.colorIndicatorShape);
        colorIndicatorText = findViewById(R.id.colorIndicatorText);
        editTextInput = findViewById(R.id.editTextInput);
        checkBoxBold = findViewById(R.id.checkBoxBold);
        checkBoxItalic = findViewById(R.id.checkBoxItalic);
        spinnerFont = findViewById(R.id.spinnerFont);
        btnConfirmCrop = findViewById(R.id.btnConfirmCrop);
        btnCancelCrop = findViewById(R.id.btnCancelCrop);

        // Настройка UI
        setupButtons();
        setupToolbarView();
        setupSeekBars();
        setupTextSettings();
        setupFontSpinner();
        updateColorIndicators();

        // Загрузка изображения
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = BitmapUtils.getBitmapFromUri(this, imageUri);
                editorView.setImageBitmap(bitmap);
                editorView.post(() -> editorView.fitImageToView());
                Log.d(TAG, "Изображение успешно загружено: " + imageUri);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки изображения", e);
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Log.w(TAG, "URI изображения отсутствует");
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Скрываем все панели настроек при запуске
        hideAllPanels();
    }

    private void setupToolbarView() {
        toolbarView.setOnToolSelectedListener(tool -> {
            Log.d(TAG, "Выбран инструмент: " + tool);

            // Сначала сбрасываем текущее состояние
            resetCurrentMode();

            switch (tool) {
                case UNDO:
                    editorView.undo();
                    hideAllPanels();
                    break;
                case REDO:
                    editorView.redo();
                    hideAllPanels();
                    break;
                case CROP:
                    if (editorView.isCropModeActive()) {
                        // Если режим обрезки уже активен, ничего не делаем
                        btnConfirmCrop.setVisibility(View.VISIBLE);
                        btnCancelCrop.setVisibility(View.VISIBLE);
                    } else {
                        editorView.startCropMode();
                        btnConfirmCrop.setVisibility(View.VISIBLE);
                        btnCancelCrop.setVisibility(View.VISIBLE);
                    }
                    hideAllPanels();
                    break;
                case ROTATE:
                    editorView.rotateImage(90);
                    hideAllPanels();
                    break;
                case FLIP:
                    editorView.flipImage();
                    hideAllPanels();
                    break;
                case DRAW:
                    currentMode = EditorMode.LINE;
                    editorView.setDrawingMode(EditorView.DrawingMode.LINE);
                    showBrushSettings();
                    break;
                case SHAPE:
                    showShapeSettings();
                    // Режим рисования будет выбран при нажатии на кнопку формы
                    break;
                case TEXT:
                    currentMode = EditorMode.TEXT;
                    editorView.setDrawingMode(EditorView.DrawingMode.TEXT);
                    showTextSettings();
                    break;
                case SAVE:
                    saveImage();
                    break;
            }
        });
    }

    private void resetCurrentMode() {
        currentMode = EditorMode.NONE;
        btnConfirmCrop.setVisibility(View.GONE);
        btnCancelCrop.setVisibility(View.GONE);
    }

    private void setupButtons() {
        Button btnRectangle = findViewById(R.id.btnRectangle);
        Button btnCircle = findViewById(R.id.btnCircle);

        btnConfirmCrop.setOnClickListener(v -> {
            Log.d(TAG, "Подтверждение обрезки");
            editorView.applyCrop();
            btnConfirmCrop.setVisibility(View.GONE);
            btnCancelCrop.setVisibility(View.GONE);
        });

        btnCancelCrop.setOnClickListener(v -> {
            Log.d(TAG, "Отмена обрезки");
            editorView.cancelCrop();
            btnConfirmCrop.setVisibility(View.GONE);
            btnCancelCrop.setVisibility(View.GONE);
        });

        btnRectangle.setOnClickListener(v -> {
            Log.d(TAG, "Выбран прямоугольник");
            currentMode = EditorMode.RECTANGLE;
            editorView.setDrawingMode(EditorView.DrawingMode.RECTANGLE);
            // Обновляем состояние кнопок
            btnRectangle.setSelected(true);
            btnCircle.setSelected(false);
            // Передаем текущий цвет для прямоугольника
            editorView.setBrushColor(currentColor);
        });

        btnCircle.setOnClickListener(v -> {
            Log.d(TAG, "Выбран круг");
            currentMode = EditorMode.CIRCLE;
            editorView.setDrawingMode(EditorView.DrawingMode.CIRCLE);
            // Обновляем состояние кнопок
            btnRectangle.setSelected(false);
            btnCircle.setSelected(true);
            // Передаем текущий цвет для круга
            editorView.setBrushColor(currentColor);
        });

        btnColor.setOnClickListener(v -> {
            Log.d(TAG, "Открытие выбора цвета для кисти");
            showColorPicker(0);
        });

        btnShapeColor.setOnClickListener(v -> {
            Log.d(TAG, "Открытие выбора цвета для фигур");
            showColorPicker(1);
        });

        btnTextColor.setOnClickListener(v -> {
            Log.d(TAG, "Открытие выбора цвета для текста");
            showColorPicker(2);
        });
    }

    private void updateColorIndicators() {
        updateColorIndicator(colorIndicatorBrush, currentColor);
        updateColorIndicator(colorIndicatorShape, currentColor);
        updateColorIndicator(colorIndicatorText, currentColor);
        Log.d(TAG, "Обновлены индикаторы цвета: #" + Integer.toHexString(currentColor));
    }

    private void updateColorIndicator(View indicator, int color) {
        if (indicator != null) {
            GradientDrawable colorCircle = new GradientDrawable();
            colorCircle.setShape(GradientDrawable.OVAL);
            colorCircle.setColor(color);
            colorCircle.setStroke(2, Color.BLACK);
            indicator.setBackground(colorCircle);
        }
    }

    private void setupSeekBars() {
        // Настройка ползунка размера кисти
        seekBarBrushSize.setMax(50); // Максимальный размер кисти
        seekBarBrushSize.setProgress(currentBrushSize - 1);
        seekBarBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBrushSize = progress + 1;
                editorView.setBrushSize(currentBrushSize);
                Log.d(TAG, "Размер кисти изменен: " + currentBrushSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Настройка ползунка размера текста
        if (seekBarTextSize != null) {
            seekBarTextSize.setMax(80); // Максимальный размер текста
            seekBarTextSize.setProgress(currentTextSize - 10);
            seekBarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentTextSize = progress + 10; // Минимальный размер текста = 10
                    updateTextDrawingProperties();
                    Log.d(TAG, "Размер текста изменен: " + currentTextSize);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void setupTextSettings() {
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentText = s.toString();
                updateTextDrawingProperties();
                Log.d(TAG, "Текст изменен: " + currentText);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBoxBold.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTextStyle();
            Log.d(TAG, "Жирный текст: " + isChecked);
        });

        checkBoxItalic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTextStyle();
            Log.d(TAG, "Курсивный текст: " + isChecked);
        });
    }

    private void updateTextDrawingProperties() {
        editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
    }

    private void updateTextStyle() {
        currentTextStyle = Typeface.NORMAL;
        if (checkBoxBold.isChecked() && checkBoxItalic.isChecked()) {
            currentTextStyle = Typeface.BOLD_ITALIC;
        } else if (checkBoxBold.isChecked()) {
            currentTextStyle = Typeface.BOLD;
        } else if (checkBoxItalic.isChecked()) {
            currentTextStyle = Typeface.ITALIC;
        }
        updateTextDrawingProperties();
    }

    private void setupFontSpinner() {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.add("Sans Serif");
        adapter.add("Serif");
        adapter.add("Monospace");
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(adapter);

        spinnerFont.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentFont = "sans-serif"; break;
                    case 1: currentFont = "serif"; break;
                    case 2: currentFont = "monospace"; break;
                }
                updateTextDrawingProperties();
                Log.d(TAG, "Шрифт изменен: " + currentFont);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showColorPicker(final int colorTargetType) {
        final int[] colors = {
                Color.BLACK, Color.WHITE, Color.RED, Color.GREEN,
                Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
                0xFFFF6600, // Оранжевый
                0xFF800080, // Пурпурный
                0xFF008080, // Бирюзовый
                0xFF808000, // Оливковый
                0xFF800000, // Бордовый
                0xFF008000, // Темно-зеленый
                0xFF000080, // Темно-синий
                0xFFFF69B4  // Розовый
        };

        final String[] colorNames = {
                "Черный", "Белый", "Красный", "Зеленый",
                "Синий", "Желтый", "Голубой", "Пурпурный",
                "Оранжевый", "Фиолетовый", "Бирюзовый", "Оливковый",
                "Бордовый", "Темно-зеленый", "Темно-синий", "Розовый"
        };

        final int[] selectedColor = {currentColor}; // Временное хранение выбранного цвета

        LinearLayout colorLayout = new LinearLayout(this);
        colorLayout.setOrientation(LinearLayout.VERTICAL);
        colorLayout.setPadding(20, 20, 20, 20);

        // Создаем горизонтальные ряды для цветов
        int rowCount = 4;
        int colorsPerRow = colors.length / rowCount;

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int colIndex = 0; colIndex < colorsPerRow; colIndex++) {
                int colorIndex = rowIndex * colorsPerRow + colIndex;

                Button colorButton = new Button(this);
                colorButton.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f));

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setColor(colors[colorIndex]);
                shape.setStroke(2, Color.BLACK);
                shape.setCornerRadius(8);
                colorButton.setBackground(shape);
                colorButton.setText("");

                final int finalColorIndex = colorIndex;
                colorButton.setOnClickListener(v -> {
                    selectedColor[0] = colors[finalColorIndex];
                    // Анимация нажатия
                    ScaleAnimation scaleAnimation = new ScaleAnimation(
                            1.0f, 0.9f, 1.0f, 0.9f,
                            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                            ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                    scaleAnimation.setDuration(200);
                    scaleAnimation.setFillAfter(false);
                    v.startAnimation(scaleAnimation);
                    Log.d(TAG, "Выбран цвет (временно): " + colorNames[finalColorIndex]);
                });

                rowLayout.addView(colorButton);
            }

            colorLayout.addView(rowLayout);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setView(colorLayout)
                .setPositiveButton("ОК", (d, which) -> {
                    currentColor = selectedColor[0];
                    switch (colorTargetType) {
                        case 0: // Кисть
                            editorView.setBrushColor(currentColor);
                            updateColorIndicator(colorIndicatorBrush, currentColor);
                            break;
                        case 1: // Фигуры
                            editorView.setBrushColor(currentColor);
                            updateColorIndicator(colorIndicatorShape, currentColor);
                            break;
                        case 2: // Текст
                            updateTextDrawingProperties();
                            updateColorIndicator(colorIndicatorText, currentColor);
                            break;
                    }
                    Log.d(TAG, "Цвет подтвержден: #" + Integer.toHexString(currentColor));
                    d.dismiss(); // Закрываем диалог
                })
                .setNegativeButton("Отмена", (d, which) -> {
                    Log.d(TAG, "Выбор цвета отменен");
                })
                .create();
        dialog.show();
    }

    private void hideAllPanels() {
        settingsPanel.setVisibility(View.GONE);
        brushSettings.setVisibility(View.GONE);
        shapeSettings.setVisibility(View.GONE);
        textSettings.setVisibility(View.GONE);
    }

    private void showBrushSettings() {
        settingsPanel.setVisibility(View.VISIBLE);
        brushSettings.setVisibility(View.VISIBLE);
        shapeSettings.setVisibility(View.GONE);
        textSettings.setVisibility(View.GONE);
    }

    private void showShapeSettings() {
        settingsPanel.setVisibility(View.VISIBLE);
        brushSettings.setVisibility(View.GONE);
        shapeSettings.setVisibility(View.VISIBLE);
        textSettings.setVisibility(View.GONE);
    }

    private void showTextSettings() {
        settingsPanel.setVisibility(View.VISIBLE);
        brushSettings.setVisibility(View.GONE);
        shapeSettings.setVisibility(View.GONE);
        textSettings.setVisibility(View.VISIBLE);
    }

    private void saveImage() {
        Bitmap finalBitmap = editorView.getFinalBitmap();
        if (finalBitmap != null) {
            // Сначала спрашиваем пользователя, желает ли он сохранить изображение
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Сохранение изображения")
                    .setMessage("Сохранить отредактированное изображение в галерею?")
                    .setPositiveButton("Сохранить", (dialog, which) -> {
                        saveImageToGallery(finalBitmap);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            Log.w(TAG, "Не удалось получить финальное изображение");
            Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "edited_image_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Изображение успешно сохранено", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Изображение сохранено: " + imageUri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения изображения", e);
                Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выход из редактора")
                .setMessage("Вы уверены, что хотите выйти? Все несохраненные изменения будут потеряны.")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (toolbarView != null) {
            toolbarView.refreshState();
        }
    }
}