package br.ufal.ic.p2.wepayu.models;

public abstract class MetodoPagamento implements Cloneable {
    public abstract String getTipo();

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}