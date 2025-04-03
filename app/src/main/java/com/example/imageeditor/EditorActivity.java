package com.example.imageeditor;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.imageeditor.models.DrawingCircle;
import com.example.imageeditor.models.DrawingLine;
import com.example.imageeditor.models.DrawingRectangle;
import com.example.imageeditor.models.DrawingText;
import com.example.imageeditor.utils.BitmapUtils;
import com.example.imageeditor.views.EditorView;
import com.example.imageeditor.views.ToolbarView;

import java.io.OutputStream;
import java.util.ArrayList;

public class EditorActivity extends AppCompatActivity {
    private EditorView editorView;
    private LinearLayout settingsPanel;
    private LinearLayout brushSettings;
    private LinearLayout shapeSettings;
    private LinearLayout textSettings;

    private SeekBar seekBarBrushSize;
    private Button btnColor;
    private Button btnShapeColor;
    private Button btnTextColor;
    private EditText editTextInput;
    private CheckBox checkBoxBold;
    private CheckBox checkBoxItalic;
    private Spinner spinnerFont;

    private int currentColor = 0xFF000000; // Черный цвет по умолчанию
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

        editorView = findViewById(R.id.editorView);

        // Инициализация панелей настроек
        settingsPanel = findViewById(R.id.settingsPanel);
        brushSettings = findViewById(R.id.brushSettings);
        shapeSettings = findViewById(R.id.shapeSettings);
        textSettings = findViewById(R.id.textSettings);

        // Инициализация элементов управления
        seekBarBrushSize = findViewById(R.id.seekBarBrushSize);
        btnColor = findViewById(R.id.btnColor);
        btnShapeColor = findViewById(R.id.btnShapeColor);
        btnTextColor = findViewById(R.id.btnTextColor);
        editTextInput = findViewById(R.id.editTextInput);
        checkBoxBold = findViewById(R.id.checkBoxBold);
        checkBoxItalic = findViewById(R.id.checkBoxItalic);
        spinnerFont = findViewById(R.id.spinnerFont);

        setupButtons();
        setupSeekBar();
        setupTextSettings();
        setupFontSpinner();

        // Загрузка изображения
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = BitmapUtils.getBitmapFromUri(this, imageUri);
                editorView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void setupButtons() {
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnCrop = findViewById(R.id.btnCrop);
        ImageButton btnRotate = findViewById(R.id.btnRotate);
        ImageButton btnFlip = findViewById(R.id.btnFlip);
        ImageButton btnDraw = findViewById(R.id.btnDraw);
        ImageButton btnShape = findViewById(R.id.btnShape);
        ImageButton btnText = findViewById(R.id.btnText);
        ImageButton btnSave = findViewById(R.id.btnSave);

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.redo();
            }
        });

        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.startCropMode();
                hideAllPanels();
            }
        });

        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.rotateImage(90);
                hideAllPanels();
            }
        });

        btnFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.flipImage();
                hideAllPanels();
            }
        });

        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMode = EditorMode.LINE;
                editorView.setDrawingMode(EditorView.DrawingMode.LINE);
                showBrushSettings();
            }
        });

        btnShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShapeSettings();
            }
        });

        btnText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMode = EditorMode.TEXT;
                editorView.setDrawingMode(EditorView.DrawingMode.TEXT);
                showTextSettings();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        // Настройка кнопок форм
        Button btnRectangle = findViewById(R.id.btnRectangle);
        Button btnCircle = findViewById(R.id.btnCircle);

        btnRectangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMode = EditorMode.RECTANGLE;
                editorView.setDrawingMode(EditorView.DrawingMode.RECTANGLE);
            }
        });

        btnCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMode = EditorMode.CIRCLE;
                editorView.setDrawingMode(EditorView.DrawingMode.CIRCLE);
            }
        });

        // Настройка кнопок выбора цвета
        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(false);
            }
        });

        btnShapeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(false);
            }
        });
        btnTextColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(true);
            }
        });
    }

    private void setupSeekBar() {
        seekBarBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBrushSize = progress + 1; // Минимальный размер 1
                editorView.setBrushSize(currentBrushSize);
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
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBoxBold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTextStyle();
            }
        });

        checkBoxItalic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTextStyle();
            }
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
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item);
        adapter.add("Sans Serif");
        adapter.add("Serif");
        adapter.add("Monospace");
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(adapter);

        spinnerFont.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentFont = "sans-serif";
                        break;
                    case 1:
                        currentFont = "serif";
                        break;
                    case 2:
                        currentFont = "monospace";
                        break;
                }
                editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showColorPicker(final boolean isTextColor) {
        final int[] colors = {
                0xFF000000, // Черный
                0xFFFFFFFF, // Белый
                0xFFFF0000, // Красный
                0xFF00FF00, // Зеленый
                0xFF0000FF, // Синий
                0xFFFFFF00, // Желтый
                0xFF00FFFF, // Голубой
                0xFFFF00FF  // Пурпурный
        };

        final String[] colorNames = {
                "Black", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Color");
        builder.setItems(colorNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentColor = colors[which];
                if (isTextColor) {
                    editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
                } else {
                    editorView.setBrushColor(currentColor);
                }
            }
        });
        builder.show();
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
                        Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}