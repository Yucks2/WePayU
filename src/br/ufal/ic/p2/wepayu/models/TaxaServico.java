package br.ufal.ic.p2.wepayu.models;

import java.time.LocalDate;

public class TaxaServico implements Cloneable {
    private LocalDate data;
    private double valor;

    public TaxaServico() {

    }

    public TaxaServico(LocalDate data, double valor) {
        this.data = data;
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        }
        catch(CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}