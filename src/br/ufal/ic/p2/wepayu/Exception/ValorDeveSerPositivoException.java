package br.ufal.ic.p2.wepayu.Exception;

public class ValorDeveSerPositivoException extends WePayUException {
    public ValorDeveSerPositivoException() {
        super("Valor deve ser positivo.");
    }
}