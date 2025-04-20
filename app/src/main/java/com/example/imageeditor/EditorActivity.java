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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imageeditor.history.DrawCommand;
import com.example.imageeditor.history.HistoryManager;
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
    private TextView txtCurrentColor;
    private TextView txtCurrentShapeColor;
    private TextView txtCurrentTextColor;
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
        txtCurrentColor = findViewById(R.id.txtCurrentColor);
        txtCurrentShapeColor = findViewById(R.id.txtCurrentShapeColor);
        txtCurrentTextColor = findViewById(R.id.txtCurrentTextColor);
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
                        btnConfirmCrop.setVisibility(View.VISIBLE);
                    } else {
                        editorView.startCropMode();
                        btnConfirmCrop.setVisibility(View.VISIBLE);
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
        GradientDrawable colorCircle = new GradientDrawable();
        colorCircle.setShape(GradientDrawable.OVAL);
        colorCircle.setColor(currentColor);

        if (txtCurrentColor != null) {
            txtCurrentColor.setBackground(colorCircle);
            txtCurrentColor.setText(getColorName(currentColor));
        }

        if (txtCurrentShapeColor != null) {
            txtCurrentShapeColor.setBackground(colorCircle);
            txtCurrentShapeColor.setText(getColorName(currentColor));
        }

        if (txtCurrentTextColor != null) {
            txtCurrentTextColor.setBackground(colorCircle);
            txtCurrentTextColor.setText(getColorName(currentColor));
        }
    }

    private String getColorName(int color) {
        switch (color) {
            case 0xFF000000: return "Черный";
            case 0xFFFFFFFF: return "Белый";
            case 0xFFFF0000: return "Красный";
            case 0xFF00FF00: return "Зеленый";
            case 0xFF0000FF: return "Синий";
            case 0xFFFFFF00: return "Желтый";
            case 0xFF00FFFF: return "Голубой";
            case 0xFFFF00FF: return "Пурпурный";
            default: return "#" + Integer.toHexString(color).substring(2).toUpperCase();
        }
    }

    private void setupSeekBar() {
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
                currentColor = colors[colorIndex];
                switch (colorTargetType) {
                    case 0: editorView.setBrushColor(currentColor); break;
                    case 1: editorView.setBrushColor(currentColor); break;
                    case 2: editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor); break;
                }
                updateColorIndicators();
                Log.d(TAG, "Выбран цвет: " + colorNames[colorIndex]);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 10, 0, 10);
            colorLayout.addView(colorButton, params);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setView(colorLayout)
                .setNegativeButton("Отмена", null)
                .create()
                .show();
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
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "edited_image_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                    if (outputStream != null) {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                        outputStream.close();
                        Toast.makeText(this, "Изображение успешно сохранено", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Изображение сохранено: " + imageUri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка сохранения изображения", e);
                    Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.w(TAG, "Не удалось получить финальное изображение");
            Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (toolbarView != null) {
            toolbarView.refreshState();
        }
    }
}