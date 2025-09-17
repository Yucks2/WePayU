package br.ufal.ic.p2.wepayu.Exception;

public class AgendaPagamentoNaoDisponivelException extends WePayUException {
    public AgendaPagamentoNaoDisponivelException() {
        super("Agenda de pagamento nao esta disponivel");
    }
}