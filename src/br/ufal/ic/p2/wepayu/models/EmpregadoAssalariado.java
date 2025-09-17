package br.ufal.ic.p2.wepayu.models;

public class EmpregadoAssalariado extends Empregado implements Cloneable {
    private double salarioMensal;

    public EmpregadoAssalariado() {

    }

    public EmpregadoAssalariado(String nome, String endereco, double salarioMensal) {
        super(nome, endereco);
        this.salarioMensal = salarioMensal;
        setAgendaPagamento("mensal $");
    }

    public double getSalarioMensal() {
        return salarioMensal;
    }

    public void setSalarioMensal(double salarioMensal) {
        this.salarioMensal = salarioMensal;
    }

    public void setSalario(double salario) {
        this.salarioMensal = salario;
    }

    @Override
    public double getSalario() {
        return this.salarioMensal;
    }

    @Override
    public String getTipo() {
        return "assalariado";
    }

    @Override
    public Object clone() {
        return super.clone();
    }
}