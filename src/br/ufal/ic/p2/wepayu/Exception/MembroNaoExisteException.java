package br.ufal.ic.p2.wepayu.Exception;

public class MembroNaoExisteException extends WePayUException {
    public MembroNaoExisteException() {
        super("Membro nao existe.");
    }
}