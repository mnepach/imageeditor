package com.example.imageeditor;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
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

    private int currentColor = 0xFF000000; // Black color by default
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
        toolbarView = findViewById(R.id.toolbarView);

        // Initialize settings panels
        settingsPanel = findViewById(R.id.settingsPanel);
        brushSettings = findViewById(R.id.brushSettings);
        shapeSettings = findViewById(R.id.shapeSettings);
        textSettings = findViewById(R.id.textSettings);

        // Initialize control elements
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

        setupButtons();
        setupToolbarView();
        setupSeekBar();
        setupTextSettings();
        setupFontSpinner();
        updateColorIndicators();

        // Load image
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                Bitmap bitmap = BitmapUtils.getBitmapFromUri(this, imageUri);
                editorView.setImageBitmap(bitmap);
                // Fix: Force image to fit to view properly on initial load
                editorView.post(new Runnable() {
                    @Override
                    public void run() {
                        editorView.fitImageToView();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void setupToolbarView() {
        toolbarView.setOnToolSelectedListener(new ToolbarView.OnToolSelectedListener() {
            @Override
            public void onToolSelected(ToolbarView.Tool tool) {
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
            }
        });
    }

    private void setupButtons() {
        // Setup shape buttons
        Button btnRectangle = findViewById(R.id.btnRectangle);
        Button btnCircle = findViewById(R.id.btnCircle);

        btnConfirmCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.applyCrop();
                btnConfirmCrop.setVisibility(View.GONE);
            }
        });

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

        // Setup color picker buttons with indicators
        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(0);
            }
        });

        btnShapeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(1);
            }
        });

        btnTextColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker(2);
            }
        });
    }

    private void updateColorIndicators() {
        // Update color indicator views
        if (txtCurrentColor != null) {
            GradientDrawable colorCircle = new GradientDrawable();
            colorCircle.setShape(GradientDrawable.OVAL);
            colorCircle.setColor(currentColor);
            txtCurrentColor.setBackground(colorCircle);
            txtCurrentColor.setText(getColorName(currentColor));
        }

        if (txtCurrentShapeColor != null) {
            GradientDrawable shapeColorCircle = new GradientDrawable();
            shapeColorCircle.setShape(GradientDrawable.OVAL);
            shapeColorCircle.setColor(currentColor);
            txtCurrentShapeColor.setBackground(shapeColorCircle);
            txtCurrentShapeColor.setText(getColorName(currentColor));
        }

        if (txtCurrentTextColor != null) {
            GradientDrawable textColorCircle = new GradientDrawable();
            textColorCircle.setShape(GradientDrawable.OVAL);
            textColorCircle.setColor(currentColor);
            txtCurrentTextColor.setBackground(textColorCircle);
            txtCurrentTextColor.setText(getColorName(currentColor));
        }
    }

    private String getColorName(int color) {
        if (color == 0xFF000000) return "Black";
        if (color == 0xFFFFFFFF) return "White";
        if (color == 0xFFFF0000) return "Red";
        if (color == 0xFF00FF00) return "Green";
        if (color == 0xFF0000FF) return "Blue";
        if (color == 0xFFFFFF00) return "Yellow";
        if (color == 0xFF00FFFF) return "Cyan";
        if (color == 0xFFFF00FF) return "Magenta";
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    private void setupSeekBar() {
        seekBarBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBrushSize = progress + 1; // Minimum size 1
                editorView.setBrushSize(currentBrushSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupTextSettings() {
        editTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentText = s.toString();
                editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showColorPicker(final int colorTargetType) {
        final int[] colors = {
                0xFF000000, // Black
                0xFFFFFFFF, // White
                0xFFFF0000, // Red
                0xFF00FF00, // Green
                0xFF0000FF, // Blue
                0xFFFFFF00, // Yellow
                0xFF00FFFF, // Cyan
                0xFFFF00FF  // Magenta
        };

        final String[] colorNames = {
                "Black", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Color");

        // Create custom layout for color items
        LinearLayout colorLayout = new LinearLayout(this);
        colorLayout.setOrientation(LinearLayout.VERTICAL);
        colorLayout.setPadding(20, 20, 20, 20);

        for (int i = 0; i < colors.length; i++) {
            final int colorIndex = i;
            Button colorButton = new Button(this);

            // Set button background color
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(colors[i]);
            shape.setStroke(2, Color.BLACK);
            shape.setCornerRadius(8);
            colorButton.setBackground(shape);

            // Set button text (color name)
            colorButton.setText(colorNames[i]);

            // Ensure text is visible (use white text on dark colors, black text on light colors)
            if (colors[i] == 0xFF000000 || colors[i] == 0xFF0000FF) {
                colorButton.setTextColor(Color.WHITE);
            } else {
                colorButton.setTextColor(Color.BLACK);
            }

            // Set button click listener
            colorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentColor = colors[colorIndex];

                    // Apply color to the appropriate target
                    switch (colorTargetType) {
                        case 0: // Brush color
                            editorView.setBrushColor(currentColor);
                            break;
                        case 1: // Shape color
                            editorView.setBrushColor(currentColor);
                            break;
                        case 2: // Text color
                            editorView.setTextDrawingProperties(currentText, currentFont, currentTextStyle, currentTextSize, currentColor);
                            break;
                    }

                    updateColorIndicators();
                }
            });

            // Add padding and margin
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 10, 0, 10);
            colorLayout.addView(colorButton, params);
        }

        builder.setView(colorLayout);
        final AlertDialog dialog = builder.create();

        // Set dialog tag on buttons to allow dismissal
        for (int i = 0; i < colorLayout.getChildCount(); i++) {
            colorLayout.getChildAt(i).setTag(dialog);
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Fix for button non-responsiveness after permission request on some devices
        if (toolbarView != null) {
            toolbarView.refreshState();
        }
    }
}