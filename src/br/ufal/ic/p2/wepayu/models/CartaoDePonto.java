package br.ufal.ic.p2.wepayu.models;

import java.time.LocalDate;

public class CartaoDePonto implements Cloneable { 
    private LocalDate data;
    private double horas;

    public CartaoDePonto() {

    }

    public CartaoDePonto(LocalDate data, double horas) {
        this.data = data;
        this.horas = horas;
    }

    public LocalDate getData() { 
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public double getHoras() {
        return horas;
    }

    public void setHoras(double horas) {
        this.horas = horas;
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