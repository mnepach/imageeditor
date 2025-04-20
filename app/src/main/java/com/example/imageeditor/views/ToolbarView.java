package com.example.imageeditor.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.imageeditor.R;

public class ToolbarView extends LinearLayout {
    public enum Tool {
        UNDO, REDO, CROP, ROTATE, FLIP, DRAW, SHAPE, TEXT, SAVE
    }

    private OnToolSelectedListener listener;

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
        LayoutInflater.from(context).inflate(R.layout.view_toolbar, this, true);

        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnCrop = findViewById(R.id.btnCrop);
        ImageButton btnRotate = findViewById(R.id.btnRotate);
        ImageButton btnFlip = findViewById(R.id.btnFlip);
        ImageButton btnDraw = findViewById(R.id.btnDraw);
        ImageButton btnShape = findViewById(R.id.btnShape);
        ImageButton btnText = findViewById(R.id.btnText);
        ImageButton btnSave = findViewById(R.id.btnSave);

        btnUndo.setOnClickListener(v -> notifyToolSelected(Tool.UNDO));
        btnRedo.setOnClickListener(v -> notifyToolSelected(Tool.REDO));
        btnCrop.setOnClickListener(v -> notifyToolSelected(Tool.CROP));
        btnRotate.setOnClickListener(v -> notifyToolSelected(Tool.ROTATE));
        btnFlip.setOnClickListener(v -> notifyToolSelected(Tool.FLIP));
        btnDraw.setOnClickListener(v -> notifyToolSelected(Tool.DRAW));
        btnShape.setOnClickListener(v -> notifyToolSelected(Tool.SHAPE));
        btnText.setOnClickListener(v -> notifyToolSelected(Tool.TEXT));
        btnSave.setOnClickListener(v -> notifyToolSelected(Tool.SAVE));
    }

    public void setOnToolSelectedListener(OnToolSelectedListener listener) {
        this.listener = listener;
    }

    private void notifyToolSelected(Tool tool) {
        if (listener != null) {
            listener.onToolSelected(tool);
        }
    }

    public void refreshState() {
        // Пустой метод, может быть расширен для обновления состояния кнопок
    }

    public interface OnToolSelectedListener {
        void onToolSelected(Tool tool);
    }
}