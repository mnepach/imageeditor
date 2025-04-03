package com.example.imageeditor.history;

public interface Command {
    void execute();
    void undo();
}