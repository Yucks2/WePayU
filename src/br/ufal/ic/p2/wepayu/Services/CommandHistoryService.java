package br.ufal.ic.p2.wepayu.Services;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.WePayUException;
import java.util.Stack;

public class CommandHistoryService {
    private interface Command {
        void execute() throws WePayUException, EmpregadoNaoExisteException;
        void undo();
    }

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    public void execute(Command command) throws WePayUException, EmpregadoNaoExisteException {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public void undo() throws WePayUException {
        if (undoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a desfazer.");
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() throws WePayUException, EmpregadoNaoExisteException {
        if (redoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a refazer.");
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }
}