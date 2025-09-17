package br.ufal.ic.p2.wepayu.Exception;

public class AgenciaNulaException extends WePayUException {
    public AgenciaNulaException() {
        super("Agencia nao pode ser nulo.");
    }
}