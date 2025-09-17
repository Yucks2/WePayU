package br.ufal.ic.p2.wepayu.models;

public class Correios extends MetodoPagamento implements Cloneable {
    @Override
    public String getTipo() {
        return "correios";
    }

    public String getDetalhes() {
        return "Correios";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}