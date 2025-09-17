package br.ufal.ic.p2.wepayu.Exception;

public class AtributoInvalidoException extends WePayUException {
    public AtributoInvalidoException(String atributo) {
        super(atributo + " invalido.");
    }
}