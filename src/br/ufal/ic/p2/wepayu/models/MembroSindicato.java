package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.List;

public class MembroSindicato implements Cloneable {
    private String idSindicato;
    private double taxaSindical;
    private List<TaxaServico> taxasDeServico = new ArrayList<>();

    public MembroSindicato() {

    }

    public MembroSindicato(String idSindicato, double taxaSindical) {
        this.idSindicato = idSindicato;
        this.taxaSindical = taxaSindical;
    }

    public String getIdSindicato() {
        return idSindicato;
    }

    public void setIdSindicato(String idSindicato) {
        this.idSindicato = idSindicato;
    }

    public double getTaxaSindical() {
        return taxaSindical;
    }

    public void setTaxaSindical(double taxaSindical) {
        this.taxaSindical = taxaSindical;
    }

    public List<TaxaServico> getTaxasDeServico() {
        return this.taxasDeServico;
    }

    public void setTaxasDeServico(List<TaxaServico> taxasDeServico) {
        this.taxasDeServico = taxasDeServico;
    }

    public void lancaTaxaServico(TaxaServico taxa) {
        this.taxasDeServico.add(taxa);
    }
    
    @Override
    public Object clone() {
        try {
            MembroSindicato cloned = (MembroSindicato) super.clone();
            cloned.taxasDeServico = new ArrayList<>();
            for(TaxaServico t : this.taxasDeServico) {
                cloned.taxasDeServico.add((TaxaServico) t.clone());
            }
            return cloned;
        }
        catch(CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}