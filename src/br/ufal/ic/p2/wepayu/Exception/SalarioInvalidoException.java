package br.ufal.ic.p2.wepayu.Exception;

public class SalarioInvalidoException extends WePayUException {
    public SalarioInvalidoException(String reason) {
        super("Salario " + reason);
    }
}