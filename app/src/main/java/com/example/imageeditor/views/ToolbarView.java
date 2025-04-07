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

    public interface OnToolSelectedListener {
        void onToolSelected(Tool tool);
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
        LayoutInflater.from(context).inflate(R.layout.view_toolbar_new, this, true);

        // Initialize buttons with appropriate IDs from the layout
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnCrop = findViewById(R.id.btnCrop);
        ImageButton btnRotate = findViewById(R.id.btnRotate);
        ImageButton btnFlip = findViewById(R.id.btnFlip);
        ImageButton btnDraw = findViewById(R.id.btnDraw);
        ImageButton btnShape = findViewById(R.id.btnShape);
        ImageButton btnText = findViewById(R.id.btnText);
        ImageButton btnSave = findViewById(R.id.btnSave);

        btnUndo.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.UNDO);
        });

        btnRedo.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.REDO);
        });

        btnCrop.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.CROP);
        });

        btnRotate.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.ROTATE);
        });

        btnFlip.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.FLIP);
        });

        btnDraw.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.DRAW);
        });

        btnShape.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.SHAPE);
        });

        btnText.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.TEXT);
        });

        btnSave.setOnClickListener(v -> {
            if (listener != null) listener.onToolSelected(Tool.SAVE);
        });
    }

    public void setOnToolSelectedListener(OnToolSelectedListener listener) {
        this.listener = listener;
    }
}