package br.ufal.ic.p2.wepayu.models;

public class Banco extends MetodoPagamento implements Cloneable {
    private String banco;
    private String agencia;
    private String contaCorrente;

    public Banco() {
        
    }

    public Banco(String banco, String agencia, String contaCorrente) {
        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public String getContaCorrente() {
        return contaCorrente;
    }

    public void setContaCorrente(String contaCorrente) {
        this.contaCorrente = contaCorrente;
    }

    @Override
    public String getTipo() {
        return "banco";
    }

    public String getDetalhes() {
        return this.banco + ", Ag. " + this.agencia + " CC " + this.contaCorrente;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}