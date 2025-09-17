package br.ufal.ic.p2.wepayu.models;

public class EmMaos extends MetodoPagamento implements Cloneable {
    @Override
    public String getTipo() {
        return "emMaos";
    }

    public String getDetalhes() {
        return "Em maos";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}