package br.ufal.ic.p2.wepayu.Services;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.*;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmpregadoService {
    private Map<String, Empregado> empregados;
    private int proximoId;
    private static final String DATA_FILE = "data.xml";

    public EmpregadoService() {
        carregarSistema();
    }
     private void carregarSistema() {
        File file = new File(DATA_FILE);
        if(file.exists() && file.length() > 0) {
            try(FileInputStream fis = new FileInputStream(DATA_FILE);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 XMLDecoder decoder = new XMLDecoder(bis)) {
                this.empregados = (Map<String, Empregado>) decoder.readObject();
                this.proximoId = (int) decoder.readObject();
            }
            catch(Exception e) {
                zerarSistema();
            }
        }
        else {
            this.empregados = new HashMap<>();
            this.proximoId = 1;
        }
    }

    public void encerrarSistema() {
        try(FileOutputStream fos = new FileOutputStream(DATA_FILE);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             XMLEncoder encoder = new XMLEncoder(bos)) {

            encoder.setPersistenceDelegate(LocalDate.class,
                new PersistenceDelegate() {
                    @Override
                    protected Expression instantiate(Object oldInstance, Encoder out) {
                        LocalDate date = (LocalDate) oldInstance;
                        return new Expression(date, LocalDate.class, "parse", new Object[]{date.toString()});
                    }
                });

            encoder.writeObject(empregados);
            encoder.writeObject(proximoId);

        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void zerarSistema() {
        if(empregados != null) {
            empregados.clear();
        }
        else {
            empregados = new HashMap<>();
        }
        proximoId = 1;
        File file = new File(DATA_FILE);
        if(file.exists()) {
            file.delete();
        }
    }


    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao, CommandHistoryService commandHistory) throws WePayUException, EmpregadoNaoExisteException {
        if(nome == null || nome.trim().isEmpty()) {
            throw new NomeNuloException();
        }
        if(endereco == null || endereco.trim().isEmpty()) {
            throw new EnderecoNuloException();
        }
        if(salario == null || salario.trim().isEmpty()) {
            throw new SalarioInvalidoException("nao pode ser nulo.");
        }
        double salarioNumerico;
        try {
            salarioNumerico = Double.parseDouble(salario.replace(",", "."));
        }
        catch(NumberFormatException e) {
            throw new SalarioInvalidoException("deve ser numerico.");
        }
        if(salarioNumerico < 0) {
            throw new SalarioInvalidoException("deve ser nao-negativo.");
        }

        Empregado novoEmpregado;
        
        if(tipo.equalsIgnoreCase("horista")) {
            if(comissao != null) {
                throw new TipoNaoAplicavelException();
            }
            novoEmpregado = new EmpregadoHorista(nome, endereco, salarioNumerico);
        }
        else if(tipo.equalsIgnoreCase("assalariado")) {
            if(comissao != null) {
                throw new TipoNaoAplicavelException();
            }
            novoEmpregado = new EmpregadoAssalariado(nome, endereco, salarioNumerico);
        }
        else if(tipo.equalsIgnoreCase("comissionado")) {
            if(comissao == null || comissao.trim().isEmpty()) {
                throw new ComissaoInvalidaException("nao pode ser nula.");
            }

            double comissaoNumerica;

            try {
                comissaoNumerica = Double.parseDouble(comissao.replace(",", "."));
            }
            catch(NumberFormatException e) {
                throw new ComissaoInvalidaException("deve ser numerica.");
            }
            if(comissaoNumerica < 0) {
                throw new ComissaoInvalidaException("deve ser nao-negativa.");
            }
            novoEmpregado = new EmpregadoComissionado(nome, endereco, salarioNumerico, comissaoNumerica);
        }
        else {
            throw new TipoInvalidoException();
        }
        String novoId = String.valueOf(proximoId++);
        this.empregados.put(novoId, novoEmpregado);
        return novoId;
    }

    public String getAtributoEmpregado(String emp, String atributo) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }

        Empregado e = empregados.get(emp);
        
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }

        switch(atributo.toLowerCase()) {
            case "nome": return e.getNome();
            case "endereco": return e.getEndereco();
            case "tipo": return e.getTipo();
            case "salario":
                return String.format("%.2f", e.getSalario()).replace('.', ',');
            case "comissao":
                if(e instanceof EmpregadoComissionado) {
                    return String.format("%.2f", ((EmpregadoComissionado) e).getTaxaDeComissao()).replace('.', ',');
                }
                throw new EmpregadoNaoComissionadoException();
            case "sindicalizado": return String.valueOf(e.isSindicalizado());
            case "metodopagamento": return e.getMetodoPagamento().getTipo();
            case "banco":
                if(e.getMetodoPagamento() instanceof Banco) {
                    return ((Banco) e.getMetodoPagamento()).getBanco();
                }
                throw new WePayUException("Empregado nao recebe em banco.");
            case "agencia":
                if(e.getMetodoPagamento() instanceof Banco) {
                    return ((Banco) e.getMetodoPagamento()).getAgencia();
                }
                throw new WePayUException("Empregado nao recebe em banco.");
            case "contacorrente":
                if(e.getMetodoPagamento() instanceof Banco) {
                    return ((Banco) e.getMetodoPagamento()).getContaCorrente();
                }
                throw new WePayUException("Empregado nao recebe em banco.");
            case "idsindicato":
                if(e.isSindicalizado()) {
                    return e.getMembroSindicato().getIdSindicato();
                }
                throw new EmpregadoNaoSindicalizadoException();
            case "taxasindical":
                if(e.isSindicalizado()) {
                    return String.format("%.2f", e.getMembroSindicato().getTaxaSindical()).replace('.', ',');
                }
                throw new EmpregadoNaoSindicalizadoException();
            default:
                throw new AtributoNaoExisteException();
        }
    }

    public String getEmpregadoPorNome(String nome, String indice) throws WePayUException {
        List<String> idsEncontrados = new ArrayList<>();
        for(Map.Entry<String, Empregado> entry : empregados.entrySet()) {
            if(entry.getValue().getNome().equals(nome)) {
                idsEncontrados.add(entry.getKey());
            }
        }

        if(idsEncontrados.isEmpty()) {
            throw new WePayUException("Nao ha empregado com esse nome.");
        }

        Collections.sort(idsEncontrados);

        int idx = Integer.parseInt(indice) - 1;
        if(idx >= 0 && idx < idsEncontrados.size()) {
            return idsEncontrados.get(idx);
        }
        else {
            throw new WePayUException("Nao ha empregado com esse nome.");
        }
    }

    public void removerEmpregado(String emp, CommandHistoryService commandHistory) throws WePayUException, EmpregadoNaoExisteException {
         if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        if(!empregados.containsKey(emp)) {
            throw new EmpregadoNaoExisteException();
        }
        empregados.remove(emp);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1, String valor2, String valor3, CommandHistoryService commandHistory) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }

        switch(atributo.toLowerCase()) {
            case "nome":
                if(valor1 == null || valor1.isEmpty()) {
                    throw new NomeNuloException();
                }
                e.setNome(valor1);
                break;
            case "endereco":
                if(valor1 == null || valor1.isEmpty()) {
                    throw new EnderecoNuloException();
                }
                e.setEndereco(valor1);
                break;
            case "metodopagamento":
                if(valor1.equalsIgnoreCase("correios")) {
                    e.setMetodoPagamento(new Correios());
                }
                else if(valor1.equalsIgnoreCase("emmaos")) {
                    e.setMetodoPagamento(new EmMaos());
                }
                 else if (valor1.equalsIgnoreCase("banco")) {
                    if (valor2 == null || valor2.isEmpty()) throw new BancoNuloException();
                    if (valor3 == null || valor3.isEmpty()) throw new AgenciaNulaException();
                    if (valor3 == null || valor3.isEmpty()) throw new ContaCorrenteNulaException();
                    e.setMetodoPagamento(new Banco(valor2, valor3, valor3));
                }
                else {
                    throw new MetodoPagamentoInvalidoException();
                }
                break;
            case "sindicalizado":
                if(valor1.equalsIgnoreCase("false")) {
                    e.setMembroSindicato(null);
                }
                 else if (valor1.equalsIgnoreCase("true")) {
                    if (valor2 == null || valor2.isEmpty()) throw new IdentificacaoNulaException("sindicato");
                    if (valor3 == null || valor3.isEmpty()) throw new TaxaSindicalInvalidaException("nao pode ser nula.");
                    
                    double taxaNumerica;
                    try {
                        taxaNumerica = Double.parseDouble(valor3.replace(",", "."));
                    } catch (NumberFormatException ex) {
                        throw new TaxaSindicalInvalidaException("deve ser numerica.");
                    }
                    if (taxaNumerica < 0) throw new TaxaSindicalInvalidaException("deve ser nao-negativa.");

                    for (Empregado outro : empregados.values()) {
                        if (outro != e && outro.isSindicalizado() && outro.getMembroSindicato().getIdSindicato().equals(valor2)) {
                            throw new WePayUException("Ha outro empregado com esta identificacao de sindicato");
                        }
                    }
                    e.setMembroSindicato(new MembroSindicato(valor2, taxaNumerica));
                }
                else if(!valor1.equalsIgnoreCase("true")) {
                    throw new ValorInvalidoException();
                }
                break;
            case "salario":
                if(valor1 == null || valor1.isEmpty()) {
                    throw new SalarioInvalidoException("nao pode ser nulo.");
                }
                double sal;
                try { 
                    sal = Double.parseDouble(valor1.replace(",", "."));
                }
                catch(Exception ex) {
                    throw new SalarioInvalidoException("deve ser numerico.");
                }
                if(sal < 0) {
                    throw new SalarioInvalidoException("deve ser nao-negativo.");
                }

                if(e instanceof EmpregadoHorista)
                {
                    ((EmpregadoHorista) e).setSalario(sal);
                }
                else if(e instanceof EmpregadoAssalariado) {
                    ((EmpregadoAssalariado) e).setSalario(sal);
                }
                else if(e instanceof EmpregadoComissionado) {
                    ((EmpregadoComissionado) e).setSalario(sal);
                }
                break;
            case "comissao":
                if(!(e instanceof EmpregadoComissionado)) {
                    throw new EmpregadoNaoComissionadoException();
                }
                if(valor1 == null || valor1.isEmpty()) {
                    throw new ComissaoInvalidaException("nao pode ser nula.");
                }
                double com;
                try {
                    com = Double.parseDouble(valor1.replace(",", "."));
                }
                catch(Exception ex) {
                    throw new ComissaoInvalidaException("deve ser numerica.");
                }
                if(com < 0) {
                    throw new ComissaoInvalidaException("deve ser nao-negativa.");
                }
                ((EmpregadoComissionado) e).setTaxaDeComissao(com);
                break;
            case "tipo":
                Empregado novoEmpregado;
                String novoTipo = valor1;
                double novoSalario = (valor2 != null && !valor2.isEmpty()) ? Double.parseDouble(valor2.replace(",", ".")) : e.getSalario();

                if(novoTipo.equalsIgnoreCase("horista")) {
                    novoEmpregado = new EmpregadoHorista(e.getNome(), e.getEndereco(), novoSalario);
                }
                else if(novoTipo.equalsIgnoreCase("assalariado")) {
                    novoEmpregado = new EmpregadoAssalariado(e.getNome(), e.getEndereco(), novoSalario);
                }
                else if(novoTipo.equalsIgnoreCase("comissionado")) {
                    if(valor2 == null || valor2.isEmpty()) {
                        throw new ComissaoInvalidaException("nao pode ser nula.");
                    }
                    double novaComissao = Double.parseDouble(valor2.replace(",", "."));
                    double salarioBase = (valor3 != null && !valor3.isEmpty()) ? Double.parseDouble(valor3.replace(",", ".")) : e.getSalario();
                    novoEmpregado = new EmpregadoComissionado(e.getNome(), e.getEndereco(), salarioBase, novaComissao);
                }
                else {
                    throw new TipoInvalidoException();
                }

                novoEmpregado.setMembroSindicato(e.getMembroSindicato());
                novoEmpregado.setMetodoPagamento(e.getMetodoPagamento());
                empregados.put(emp, novoEmpregado);
                break;
            default:
                throw new AtributoNaoExisteException();
        }
    }
     public int getNumeroDeEmpregados(){
        return empregados.size();
    }
     public Map<String, Empregado> getEmpregados() {
        return empregados;
    }
}