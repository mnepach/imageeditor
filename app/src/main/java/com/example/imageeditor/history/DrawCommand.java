package com.example.imageeditor.history;

import com.example.imageeditor.models.DrawingObject;

import java.util.ArrayList;
import java.util.List;

public class DrawCommand implements Command {
    private List<DrawingObject> drawingObjects;
    private DrawingObject drawingObject;
    private boolean isAdd;

    // Команда для добавления объекта рисования
    public DrawCommand(List<DrawingObject> drawingObjects, DrawingObject drawingObject) {
        this.drawingObjects = drawingObjects;
        this.drawingObject = drawingObject;
        this.isAdd = true;
    }

    // Команда для удаления объекта рисования
    public DrawCommand(List<DrawingObject> drawingObjects, DrawingObject drawingObject, boolean isAdd) {
        this.drawingObjects = drawingObjects;
        this.drawingObject = drawingObject;
        this.isAdd = isAdd;
    }

    @Override
    public void execute() {
        if (isAdd) {
            if (!drawingObjects.contains(drawingObject)) {
                drawingObjects.add(drawingObject);
            }
        } else {
            drawingObjects.remove(drawingObject);
        }
    }

    @Override
    public void undo() {
        if (isAdd) {
            drawingObjects.remove(drawingObject);
        } else {
            if (!drawingObjects.contains(drawingObject)) {
                drawingObjects.add(drawingObject);
            }
        }
    }
}