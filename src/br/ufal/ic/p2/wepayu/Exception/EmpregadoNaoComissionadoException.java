package br.ufal.ic.p2.wepayu.Exception;

public class EmpregadoNaoComissionadoException extends WePayUException {
    public EmpregadoNaoComissionadoException() {
        super("Empregado nao eh comissionado.");
    }
}