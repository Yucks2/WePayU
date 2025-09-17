package br.ufal.ic.p2.wepayu.Exception;

public class MetodoPagamentoInvalidoException extends WePayUException {
    public MetodoPagamentoInvalidoException() {
        super("Metodo de pagamento invalido.");
    }
}