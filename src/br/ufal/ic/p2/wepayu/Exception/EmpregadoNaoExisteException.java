package br.ufal.ic.p2.wepayu.Exception;

public class EmpregadoNaoExisteException extends Exception{
    public EmpregadoNaoExisteException(){
        super("Empregado nao existe.");
    }
}
