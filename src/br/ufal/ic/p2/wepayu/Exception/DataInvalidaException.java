package br.ufal.ic.p2.wepayu.Exception;

public class DataInvalidaException extends WePayUException {
    public DataInvalidaException(String tipoData) {
        super(tipoData.isEmpty() ? "Data invalida." : "Data " + tipoData + " invalida.");
    }
}