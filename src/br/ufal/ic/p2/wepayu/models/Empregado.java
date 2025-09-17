package br.ufal.ic.p2.wepayu.models;

import java.time.LocalDate;

public abstract class Empregado implements Cloneable {
    private String nome;
    private String endereco;
    private MembroSindicato membroSindicato;
    private MetodoPagamento metodoPagamento;
    private String agendaPagamento;
    private LocalDate ultimoPagamento;
    private LocalDate dataContratacao;

    public Empregado() {

    }

    public Empregado(String nome, String endereco) {
        this.nome = nome;
        this.endereco = endereco;
        this.membroSindicato = null;
        this.metodoPagamento = new EmMaos();
        if(!(this instanceof EmpregadoHorista)) {
             this.dataContratacao = LocalDate.of(2005, 1, 1);
        }
        this.ultimoPagamento = this.dataContratacao != null ? this.dataContratacao.minusDays(1) : LocalDate.of(2004, 12, 31);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public MembroSindicato getMembroSindicato() {
        return membroSindicato;
    }

    public void setMembroSindicato(MembroSindicato membroSindicato) {
        this.membroSindicato = membroSindicato;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public boolean isSindicalizado() {
        return this.membroSindicato != null;
    }

    public void lancaCartao(CartaoDePonto cartao) {

    }

    public String getAgendaPagamento() {
        return agendaPagamento;
    }

    public void setAgendaPagamento(String agenda) {
        this.agendaPagamento = agenda;
    }

    public LocalDate getUltimoPagamento() {
        return ultimoPagamento;
    }

    public void setUltimoPagamento(LocalDate data) {
        this.ultimoPagamento = data;
    }

    public LocalDate getDataContratacao() {
        return dataContratacao;
    }

    public void setDataContratacao(LocalDate dataContratacao) {
        this.dataContratacao = dataContratacao;
    }
    
    public abstract String getTipo();
    public abstract double getSalario();

    @Override
    public Object clone() {
        try {
            Empregado cloned = (Empregado) super.clone();
            if (this.membroSindicato != null) {
                cloned.membroSindicato = (MembroSindicato) this.membroSindicato.clone();
            }
             if (this.metodoPagamento != null) {
                cloned.metodoPagamento = (MetodoPagamento) this.metodoPagamento.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }
}