package br.ufal.ic.p2.wepayu.Exception;

public class TaxaSindicalInvalidaException extends WePayUException {
    public TaxaSindicalInvalidaException(String reason) {
        super("Taxa sindical " + reason);
    }
}