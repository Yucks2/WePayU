package br.ufal.ic.p2.wepayu.Exception;

public class ComissaoInvalidaException extends WePayUException {
    public ComissaoInvalidaException(String reason) {
        super("Comissao " + reason);
    }
}