package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.List;

public class EmpregadoComissionado extends EmpregadoAssalariado implements Cloneable {
    private double taxaDeComissao;
    private List<ResultadoDeVenda> vendas = new ArrayList<>();

    public EmpregadoComissionado() {

    }

    public EmpregadoComissionado(String nome, String endereco, double salarioMensal, double taxaDeComissao) {
        super(nome, endereco, salarioMensal);
        this.taxaDeComissao = taxaDeComissao;
        setAgendaPagamento("semanal 2 5");
    }

    public double getTaxaDeComissao() {
        return taxaDeComissao;
    }

    public void setTaxaDeComissao(double taxa) {
        this.taxaDeComissao = taxa;
    }

    public List<ResultadoDeVenda> getVendas() {
        return this.vendas;
    }

    public void setVendas(List<ResultadoDeVenda> vendas) {
        this.vendas = vendas;
    }

    public void lancaVenda(ResultadoDeVenda venda) {
        this.vendas.add(venda);
    }
    
    @Override
    public String getTipo() {
        return "comissionado";
    }

    @Override
    public Object clone() {
        EmpregadoComissionado cloned = (EmpregadoComissionado) super.clone();
        cloned.vendas = new ArrayList<>();
        for(ResultadoDeVenda v : this.vendas){
            cloned.vendas.add((ResultadoDeVenda) v.clone());
        }
        return cloned;
    }
}