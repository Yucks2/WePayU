package br.ufal.ic.p2.wepayu;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.WePayUException;

public class Facade {
    private SistemaFolha sistema = new SistemaFolha();
    private boolean sistemaEncerrado = false;

    private void verificarSistemaEncerrado() throws WePayUException {
        if(sistemaEncerrado) {
            throw new WePayUException("Nao pode dar comandos depois de encerrarSistema.");
        }
    }

    public void zerarSistema() {
        sistema.zerarSistema();
        sistemaEncerrado = false;
    }

    public void encerrarSistema() {
        sistema.encerrarSistema();
        sistemaEncerrado = true;
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        if(tipo.equalsIgnoreCase("comissionado")) {
            throw new WePayUException("Tipo nao aplicavel.");
        }
        return sistema.criarEmpregado(nome, endereco, tipo, salario, null);
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.criarEmpregado(nome, endereco, tipo, salario, comissao);
    }
    
    public String getAtributoEmpregado(String emp, String atributo) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.getAtributoEmpregado(emp, atributo);
    }

    public String getEmpregadoPorNome(String nome, int indice) throws WePayUException {
        verificarSistemaEncerrado();
        return sistema.getEmpregadoPorNome(nome, Integer.toString(indice));
    }

    public void removerEmpregado(String emp) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.removerEmpregado(emp);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.alteraEmpregado(emp, atributo, valor1, null, null);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1, String valor2) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.alteraEmpregado(emp, atributo, valor1, valor2, null);
    }

    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.alteraEmpregadoSindicalizado(emp, valor, idSindicato, taxaSindical);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        if(atributo.equalsIgnoreCase("metodoPagamento") && valor1.equalsIgnoreCase("banco")) {
            sistema.alteraEmpregadoPagamentoBanco(emp, banco, agencia, contaCorrente);
        }
        else {
             throw new WePayUException("Comando invalido.");
        }
    }

    public void lancaCartao(String emp, String data, String horas) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.lancaCartao(emp, data, horas);
    }
    
    public void lancaVenda(String emp, String data, String valor) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.lancaVenda(emp, data, valor);
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws WePayUException {
        verificarSistemaEncerrado();
        sistema.lancaTaxaServico(membro, data, valor);
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.getHorasTrabalhadas(emp, dataInicial, dataFinal, false);
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.getHorasTrabalhadas(emp, dataInicial, dataFinal, true);
    }
    
    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.getVendasRealizadas(emp, dataInicial, dataFinal);
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        return sistema.getTaxasServico(emp, dataInicial, dataFinal);
    }

    public String totalFolha(String data) throws Exception {
        verificarSistemaEncerrado();
        return sistema.totalFolha(data);
    }

    public void rodaFolha(String data, String saida) throws Exception {
        verificarSistemaEncerrado();
        sistema.rodaFolha(data, saida);
    }

    public void undo() throws WePayUException {
        verificarSistemaEncerrado();
        sistema.undo();
    }
    
    public void redo() throws WePayUException, EmpregadoNaoExisteException {
        verificarSistemaEncerrado();
        sistema.redo();
    }

    public int getNumeroDeEmpregados(){
        return sistema.getNumeroDeEmpregados();
    }
}