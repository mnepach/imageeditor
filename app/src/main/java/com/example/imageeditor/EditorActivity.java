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

    private int currentColor = 0xFF000000; // Черный по умолчанию
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

        // Настройка UI
        setupButtons();
        setupToolbarView();
        setupSeekBar();
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
    }

    private void setupToolbarView() {
        toolbarView.setOnToolSelectedListener(tool -> {
            Log.d(TAG, "Выбран инструмент: " + tool);
            hideAllPanels();
            btnConfirmCrop.setVisibility(View.GONE);

            switch (tool) {
                case UNDO:
                    editorView.undo();
                    break;
                case REDO:
                    editorView.redo();
                    break;
                case CROP:
                    if (editorView.isCropModeActive()) {
                        btnConfirmCrop.setVisibility(View.VISIBLE);
                    } else {
                        editorView.startCropMode();
                        btnConfirmCrop.setVisibility(View.VISIBLE);
                    }
                    break;
                case ROTATE:
                    editorView.rotateImage(90);
                    break;
                case FLIP:
                    editorView.flipImage();
                    break;
                case DRAW:
                    currentMode = EditorMode.LINE;
                    editorView.setDrawingMode(EditorView.DrawingMode.LINE);
                    showBrushSettings();
                    break;
                case SHAPE:
                    showShapeSettings();
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

    private void setupButtons() {
        Button btnRectangle = findViewById(R.id.btnRectangle);
        Button btnCircle = findViewById(R.id.btnCircle);

        btnConfirmCrop.setOnClickListener(v -> {
            Log.d(TAG, "Подтверждение обрезки");
            editorView.applyCrop();
            btnConfirmCrop.setVisibility(View.GONE);
        });

        btnRectangle.setOnClickListener(v -> {
            Log.d(TAG, "Выбран прямоугольник");
            currentMode = EditorMode.RECTANGLE;
            editorView.setDrawingMode(EditorView.DrawingMode.RECTANGLE);
        });

        btnCircle.setOnClickListener(v -> {
            Log.d(TAG, "Выбран круг");
            currentMode = EditorMode.CIRCLE;
            editorView.setDrawingMode(EditorView.DrawingMode.CIRCLE);
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
        updateColorIndicator(colorIndicatorBrush);
        updateColorIndicator(colorIndicatorShape);
        updateColorIndicator(colorIndicatorText);
        Log.d(TAG, "Обновлены индикаторы цвета: #" + Integer.toHexString(currentColor));
    }

    private void updateColorIndicator(View indicator) {
        if (indicator != null) {
            GradientDrawable colorCircle = new GradientDrawable();
            colorCircle.setShape(GradientDrawable.OVAL);
            colorCircle.setColor(currentColor);
            colorCircle.setStroke(2, Color.BLACK);
            indicator.setBackground(colorCircle);
        }
    }

    private void setupSeekBar() {
        // Устанавливаем начальное значение
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
    }

    private void setupTextSettings() {
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentText = s.toString();
                editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
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

    private void updateTextStyle() {
        currentTextStyle = Typeface.NORMAL;
        if (checkBoxBold.isChecked() && checkBoxItalic.isChecked()) {
            currentTextStyle = Typeface.BOLD_ITALIC;
        } else if (checkBoxBold.isChecked()) {
            currentTextStyle = Typeface.BOLD;
        } else if (checkBoxItalic.isChecked()) {
            currentTextStyle = Typeface.ITALIC;
        }
        editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
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
                editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
                Log.d(TAG, "Шрифт изменен: " + currentFont);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showColorPicker(final int colorTargetType) {
        final int[] colors = {
                0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF
        };
        final String[] colorNames = {
                "Черный", "Белый", "Красный", "Зеленый",
                "Синий", "Желтый", "Голубой", "Пурпурный"
        };
        final int[] selectedColor = {currentColor}; // Временное хранение выбранного цвета

        LinearLayout colorLayout = new LinearLayout(this);
        colorLayout.setOrientation(LinearLayout.VERTICAL);
        colorLayout.setPadding(20, 20, 20, 20);

        for (int i = 0; i < colors.length; i++) {
            final int colorIndex = i;
            Button colorButton = new Button(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(colors[i]);
            shape.setStroke(2, Color.BLACK);
            shape.setCornerRadius(8);
            colorButton.setBackground(shape);
            colorButton.setText(colorNames[i]);
            colorButton.setTextColor(colors[i] == 0xFF000000 || colors[i] == 0xFF0000FF ? Color.WHITE : Color.BLACK);

            colorButton.setOnClickListener(v -> {
                selectedColor[0] = colors[colorIndex];
                // Анимация нажатия
                ScaleAnimation scaleAnimation = new ScaleAnimation(
                        1.0f, 0.9f, 1.0f, 0.9f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(200);
                scaleAnimation.setFillAfter(false);
                v.startAnimation(scaleAnimation);
                Log.d(TAG, "Выбран цвет (временно): " + colorNames[colorIndex]);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 10, 0, 10);
            colorLayout.addView(colorButton, params);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setView(colorLayout)
                .setPositiveButton("ОК", (d, which) -> {
                    currentColor = selectedColor[0];
                    switch (colorTargetType) {
                        case 0: // Кисть
                            editorView.setBrushColor(currentColor);
                            break;
                        case 1: // Фигура
                            editorView.setBrushColor(currentColor);
                            break;
                        case 2: // Текст
                            editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
                            break;
                    }
                    updateColorIndicators();
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