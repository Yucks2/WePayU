package br.ufal.ic.p2.wepayu.Exception;

public class EmpregadoNaoHoristaException extends WePayUException {
    public EmpregadoNaoHoristaException() {
        super("Empregado nao eh horista.");
    }
}