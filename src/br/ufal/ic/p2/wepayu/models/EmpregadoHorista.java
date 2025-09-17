package br.ufal.ic.p2.wepayu.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmpregadoHorista extends Empregado implements Cloneable {
    private double salarioPorHora;
    private List<CartaoDePonto> cartoes = new ArrayList<>();

    public EmpregadoHorista() {

    }

    public EmpregadoHorista(String nome, String endereco, double salarioPorHora) {
        super(nome, endereco);
        this.salarioPorHora = salarioPorHora;
        setAgendaPagamento("semanal 5");
    }
    
    public double getSalarioPorHora() {
        return salarioPorHora;
    }

    public void setSalarioPorHora(double salarioPorHora) {
        this.salarioPorHora = salarioPorHora;
    }

    public List<CartaoDePonto> getCartoes() {
        return cartoes;
    }

    public void setCartoes(List<CartaoDePonto> cartoes) {
        this.cartoes = cartoes;
    }

    @Override
    public void lancaCartao(CartaoDePonto cartao) {
        if(this.getDataContratacao() == null) {
            this.setDataContratacao(cartao.getData());
            this.setUltimoPagamento(cartao.getData().minusDays(1));
        }
        this.cartoes.add(cartao);
    }

    public void setSalario(double salario) {
        this.salarioPorHora = salario;
    }

    @Override
    public double getSalario() {
        return this.salarioPorHora;
    }

    @Override
    public String getTipo() {
        return "horista";
    }

    @Override
    public Object clone() {
        EmpregadoHorista cloned = (EmpregadoHorista) super.clone();
        cloned.cartoes = new ArrayList<>();
        for(CartaoDePonto c : this.cartoes) {
            cloned.cartoes.add((CartaoDePonto) c.clone());
        }
        return cloned;
    }
}