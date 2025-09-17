package br.ufal.ic.p2.wepayu.Exception;

public class NomeNuloException extends WePayUException {
    public NomeNuloException() {
        super("Nome nao pode ser nulo.");
    }
}