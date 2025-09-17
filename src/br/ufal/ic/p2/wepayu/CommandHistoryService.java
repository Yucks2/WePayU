package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.WePayUException;
import java.util.Stack;
import java.util.function.Consumer;

public class CommandHistoryService {

    private final Stack<Runnable> undoStack = new Stack<>();
    private final Stack<Runnable> redoStack = new Stack<>();

    public void execute(Runnable commandAction, Runnable undoAction) {
        commandAction.run();
        undoStack.push(undoAction);
        redoStack.clear();
    }

    public void undo() throws WePayUException {
        if(undoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a desfazer.");
        }
        Runnable undoAction = undoStack.pop();
        undoAction.run();
    }

    public void redo() throws WePayUException {
        if(redoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a refazer.");
        }
        Runnable redoAction = redoStack.pop();
        redoAction.run();
    }
}