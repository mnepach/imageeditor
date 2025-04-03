package com.example.imageeditor.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.example.imageeditor.R;

public class ToolbarView extends LinearLayout {

    private EditorView editorView;
    private View colorPickerLayout;
    private View strokeWidthLayout;
    private View textOptionsLayout;

    private ImageButton btnPan;
    private ImageButton btnDraw;
    private ImageButton btnRect;
    private ImageButton btnCircle;
    private ImageButton btnText;
    private ImageButton btnColorPicker;
    private ImageButton btnStrokeWidth;
    private ImageButton btnUndo;
    private ImageButton btnRedo;
    private ImageButton btnRotateLeft;
    private ImageButton btnRotateRight;
    private ImageButton btnFlipHorizontal;
    private ImageButton btnFlipVertical;

    private View btnRed;
    private View btnGreen;
    private View btnBlue;
    private View btnBlack;
    private View btnWhite;

    private SeekBar seekBarStrokeWidth;

    private Button btnBold;
    private Button btnItalic;
    private Button btnFontSans;
    private Button btnFontSerif;
    private Button btnFontMono;

    private boolean isBold = false;
    private boolean isItalic = false;

    public ToolbarView(Context context) {
        super(context);
        init(context);
    }

    public ToolbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ToolbarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);

        // Инфлейт основного макета
        LayoutInflater.from(context).inflate(R.layout.view_toolbar, this, true);

        // Инициализация вложенных макетов
        colorPickerLayout = findViewById(R.id.color_picker_layout);
        strokeWidthLayout = findViewById(R.id.stroke_width_layout);
        textOptionsLayout = findViewById(R.id.text_options_layout);

        // Скрытие вложенных макетов по умолчанию
        colorPickerLayout.setVisibility(GONE);
        strokeWidthLayout.setVisibility(GONE);
        textOptionsLayout.setVisibility(GONE);

        // Инициализация кнопок управления режимами
        btnPan = findViewById(R.id.btn_pan);
        btnDraw = findViewById(R.id.btn_draw);
        btnRect = findViewById(R.id.btn_rect);
        btnCircle = findViewById(R.id.btn_circle);
        btnText = findViewById(R.id.btn_text);
        btnColorPicker = findViewById(R.id.btn_color_picker);
        btnStrokeWidth = findViewById(R.id.btn_stroke_width);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);
        btnRotateLeft = findViewById(R.id.btn_rotate_left);
        btnRotateRight = findViewById(R.id.btn_rotate_right);
        btnFlipHorizontal = findViewById(R.id.btn_flip_horizontal);
        btnFlipVertical = findViewById(R.id.btn_flip_vertical);

        // Инициализация кнопок выбора цвета
        btnRed = findViewById(R.id.btn_color_red);
        btnGreen = findViewById(R.id.btn_color_green);
        btnBlue = findViewById(R.id.btn_color_blue);
        btnBlack = findViewById(R.id.btn_color_black);
        btnWhite = findViewById(R.id.btn_color_white);

        // Инициализация ползунка толщины линии
        seekBarStrokeWidth = findViewById(R.id.seek_bar_stroke_width);

        // Инициализация кнопок текстовых стилей
        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnFontSans = findViewById(R.id.btn_font_sans);
        btnFontSerif = findViewById(R.id.btn_font_serif);
        btnFontMono = findViewById(R.id.btn_font_mono);

        setupListeners();
    }

    // Установка слушателей событий
    private void setupListeners() {

        // Режимы редактирования
        btnPan.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setMode(EditorView.MODE_PAN);
                updateModeButtons(btnPan);
            }
        });

        btnDraw.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setMode(EditorView.MODE_DRAW);
                updateModeButtons(btnDraw);
            }
        });

        btnRect.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setMode(EditorView.MODE_RECT);
                updateModeButtons(btnRect);
            }
        });

        btnCircle.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setMode(EditorView.MODE_CIRCLE);
                updateModeButtons(btnCircle);
            }
        });

        btnText.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setMode(EditorView.MODE_TEXT);
                updateModeButtons(btnText);
                showTextOptions(true);
            }
        });

        // Показ/скрытие палитры цветов
        btnColorPicker.setOnClickListener(v -> {
            boolean isVisible = colorPickerLayout.getVisibility() == VISIBLE;
            colorPickerLayout.setVisibility(isVisible ? GONE : VISIBLE);
            strokeWidthLayout.setVisibility(GONE);
            textOptionsLayout.setVisibility(GONE);
        });

        // Показ/скрытие настройки толщины линии
        btnStrokeWidth.setOnClickListener(v -> {
            boolean isVisible = strokeWidthLayout.getVisibility() == VISIBLE;
            strokeWidthLayout.setVisibility(isVisible ? GONE : VISIBLE);
            colorPickerLayout.setVisibility(GONE);
            textOptionsLayout.setVisibility(GONE);
        });

        // Отмена/повтор действия
        btnUndo.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.undo();
            }
        });

        btnRedo.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.redo();
            }
        });

        // Поворот изображения
        btnRotateLeft.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.rotateImage(-90);
            }
        });

        btnRotateRight.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.rotateImage(90);
            }
        });

        // Отзеркаливание изображения
        btnFlipHorizontal.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.flipImage(true);
            }
        });

        btnFlipVertical.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.flipImage(false);
            }
        });

        // Выбор цвета
        btnRed.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setColor(Color.RED);
                colorPickerLayout.setVisibility(GONE);
            }
        });

        btnGreen.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setColor(Color.GREEN);
                colorPickerLayout.setVisibility(GONE);
            }
        });

        btnBlue.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setColor(Color.BLUE);
                colorPickerLayout.setVisibility(GONE);
            }
        });

        btnBlack.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setColor(Color.BLACK);
                colorPickerLayout.setVisibility(GONE);
            }
        });

        btnWhite.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setColor(Color.WHITE);
                colorPickerLayout.setVisibility(GONE);
            }
        });

        // Изменение толщины линии
        seekBarStrokeWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (editorView != null && fromUser) {
                    float strokeWidth = progress + 1; // Минимум 1px
                    editorView.setStrokeWidth(strokeWidth);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Не используется
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                strokeWidthLayout.setVisibility(GONE);
            }
        });

        // Настройки текста
        btnBold.setOnClickListener(v -> {
            isBold = !isBold;
            updateTextStyleButtons();
            if (editorView != null) {
                editorView.setTextStyle(isBold, isItalic);
            }
        });

        btnItalic.setOnClickListener(v -> {
            isItalic = !isItalic;
            updateTextStyleButtons();
            if (editorView != null) {
                editorView.setTextStyle(isBold, isItalic);
            }
        });

        btnFontSans.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setTypeface(Typeface.SANS_SERIF);
                updateFontButtons(btnFontSans);
            }
        });

        btnFontSerif.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setTypeface(Typeface.SERIF);
                updateFontButtons(btnFontSerif);
            }
        });

        btnFontMono.setOnClickListener(v -> {
            if (editorView != null) {
                editorView.setTypeface(Typeface.MONOSPACE);
                updateFontButtons(btnFontMono);
            }
        });
    }

    // Обновление внешнего вида кнопок режимов
    private void updateModeButtons(View selectedButton) {
        btnPan.setSelected(btnPan == selectedButton);
        btnDraw.setSelected(btnDraw == selectedButton);
        btnRect.setSelected(btnRect == selectedButton);
        btnCircle.setSelected(btnCircle == selectedButton);
        btnText.setSelected(btnText == selectedButton);

        // Показать опции текста только если выбран режим текста
        showTextOptions(btnText == selectedButton);
    }

    // Обновление внешнего вида кнопок стиля текста
    private void updateTextStyleButtons() {
        btnBold.setSelected(isBold);
        btnItalic.setSelected(isItalic);
    }

    // Обновление внешнего вида кнопок шрифтов
    private void updateFontButtons(View selectedButton) {
        btnFontSans.setSelected(btnFontSans == selectedButton);
        btnFontSerif.setSelected(btnFontSerif == selectedButton);
        btnFontMono.setSelected(btnFontMono == selectedButton);
    }

    // Показать/скрыть опции текста
    private void showTextOptions(boolean show) {
        textOptionsLayout.setVisibility(show ? VISIBLE : GONE);
        // Скрыть другие панели
        if (show) {
            colorPickerLayout.setVisibility(GONE);
            strokeWidthLayout.setVisibility(GONE);
        }
    }

    // Установка ссылки на EditorView
    public void setEditorView(EditorView editorView) {
        this.editorView = editorView;
    }

    // Установка текста для текстового режима
    public void setTextForEditor(String text) {
        if (editorView != null) {
            editorView.setText(text);
        }
    }
}