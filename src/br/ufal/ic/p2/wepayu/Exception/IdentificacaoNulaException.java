package br.ufal.ic.p2.wepayu.Exception;

public class IdentificacaoNulaException extends WePayUException {
    public IdentificacaoNulaException(String tipo) {
        super("Identificacao do " + tipo + " nao pode ser nula.");
    }
}