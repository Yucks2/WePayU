package br.ufal.ic.p2.wepayu.Exception;

public class ValorInvalidoException extends WePayUException {
    public ValorInvalidoException() {
        super("Valor deve ser true ou false.");
    }
}