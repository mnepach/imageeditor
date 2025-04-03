package com.example.imageeditor.history;

import java.util.Stack;

public class HistoryManager {
    private Stack<Command> undoStack;
    private Stack<Command> redoStack;

    public HistoryManager() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
    }

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // После новой команды очищаем стек redo
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (canUndo()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    public void redo() {
        if (canRedo()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}