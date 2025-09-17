package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.*;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class SistemaFolha {
    private Map<String, Empregado> empregados = new HashMap<>();
    private int proximoId = 1;
    private static final String DATA_FILE = "data.xml";

    private Stack<SistemaFolhaMemento> undoStack = new Stack<>();
    private Stack<SistemaFolhaMemento> redoStack = new Stack<>();

    public SistemaFolha() {
        carregarSistema();
    }

    private SistemaFolhaMemento createMemento() {
        return new SistemaFolhaMemento(this.empregados, this.proximoId);
    }

    private void restoreState(SistemaFolhaMemento memento) {
        this.empregados = memento.getEmpregados();
        this.proximoId = memento.getProximoId();
    }

    private void saveState() {
        undoStack.push(createMemento());
        redoStack.clear();
    }

    public void undo() throws WePayUException {
        if(undoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a desfazer.");
        }
        redoStack.push(createMemento());
        restoreState(undoStack.pop());
    }

    public void redo() throws WePayUException {
        if(redoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a refazer.");
        }
        undoStack.push(createMemento());
        restoreState(redoStack.pop());
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
        saveState();
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

    private boolean ehDiaDePagamento(Empregado e, LocalDate data) {
        if(e.getDataContratacao() != null && data.isBefore(e.getDataContratacao())) {
            return false;
        }

        if(e instanceof EmpregadoHorista) {
            return data.getDayOfWeek() == DayOfWeek.FRIDAY;
        }
        if(e instanceof EmpregadoAssalariado && !(e instanceof EmpregadoComissionado)) {
            LocalDate ultimoDiaUtil = data.with(TemporalAdjusters.lastDayOfMonth());
            while (ultimoDiaUtil.getDayOfWeek() == DayOfWeek.SATURDAY || ultimoDiaUtil.getDayOfWeek() == DayOfWeek.SUNDAY) {
                ultimoDiaUtil = ultimoDiaUtil.minusDays(1);
            }
            return data.equals(ultimoDiaUtil);
        }
        if(e instanceof EmpregadoComissionado) {
            LocalDate dataContrato = e.getDataContratacao();
            LocalDate primeiroPagamento = dataContrato.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
            if(data.isBefore(primeiroPagamento)) {
                return false;
            }
            long daysBetween = ChronoUnit.DAYS.between(primeiroPagamento, data);
            return daysBetween % 14 == 0 && data.getDayOfWeek() == DayOfWeek.FRIDAY;
        }
        return false;
    }

    private String formatarValor(double valor) {
        return String.format("%.2f", valor).replace('.', ',');
    }

    public String totalFolha(String data) throws Exception {
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/yyyy");
        LocalDate dataFolha = LocalDate.parse(data, formatador);
        double total = 0;

        for(Empregado e : empregados.values()) {
            if(ehDiaDePagamento(e, dataFolha)) {
                Object[] pagamento = calcularPagamento(e, dataFolha);
                total += (double)pagamento[0];
            }
        }
        return formatarValor(total);
    }

    private Object[] calcularPagamento(Empregado e, LocalDate dataFim) throws EmpregadoNaoExisteException, WePayUException {
        LocalDate dataInicio = (e.getUltimoPagamento() == null) ? LocalDate.of(2004, 12, 31) : e.getUltimoPagamento();
        dataInicio = dataInicio.plusDays(1);

        double salarioBruto = 0, descontos = 0, horasNormais = 0, horasExtras = 0, fixo = 0, vendas = 0, comissao = 0;

        if(e instanceof EmpregadoHorista) {
            EmpregadoHorista h = (EmpregadoHorista) e;
            for(CartaoDePonto c : h.getCartoes()) {
                if(!c.getData().isBefore(dataInicio) && !c.getData().isAfter(dataFim)) {
                    double horasDoDia = c.getHoras();
                    horasNormais += Math.min(horasDoDia, 8);
                    horasExtras += Math.max(0, horasDoDia - 8);
                }
            }
            salarioBruto = (horasNormais * h.getSalario()) + (horasExtras * h.getSalario() * 1.5);
        }
        else if(e instanceof EmpregadoComissionado) {
            EmpregadoComissionado c = (EmpregadoComissionado) e;
            fixo = (c.getSalario() * 12) / 26.0;
            fixo = Math.floor((fixo * 100) + 1e-9) / 100.0;
            for(ResultadoDeVenda v : c.getVendas()) {
                if(!v.getData().isBefore(dataInicio) && !v.getData().isAfter(dataFim)) {
                    vendas += v.getValor();
                    comissao += v.getValor() * c.getTaxaDeComissao();
                }
            }
            comissao = Math.floor((comissao * 100) + 1e-9) / 100.0;
            salarioBruto = fixo + comissao;
        }
        else if(e instanceof EmpregadoAssalariado) {
            salarioBruto = e.getSalario();
        }
        
        salarioBruto = Math.floor((salarioBruto * 100) + 1e-9) / 100.0;
        
        if(e.isSindicalizado()) {
            MembroSindicato membro = e.getMembroSindicato();
            double taxaSindicalDiaria = membro.getTaxaSindical();
            double taxaSindicalTotal = 0;
            
            long dias;
            if(e.getUltimoPagamento() == null) {
                dias = ChronoUnit.DAYS.between(LocalDate.of(2005, 1, 1), dataFim) + 1;
            }
            else {
                dias = ChronoUnit.DAYS.between(e.getUltimoPagamento(), dataFim);
            }

            if(e instanceof EmpregadoAssalariado && !(e instanceof EmpregadoComissionado)) {
                 dias = dataFim.lengthOfMonth();
            }
            
            taxaSindicalTotal = dias * taxaSindicalDiaria;
           
            descontos += taxaSindicalTotal;

            for(TaxaServico t : membro.getTaxasDeServico()) {
                if(!t.getData().isBefore(dataInicio) && !t.getData().isAfter(dataFim)) {
                    descontos += t.getValor();
                }
            }
        }

        descontos = Math.floor((descontos * 100) + 1e-9) / 100.0;

        if(salarioBruto < descontos) {
            descontos = salarioBruto;
        }
        return new Object[]{salarioBruto, descontos, salarioBruto - descontos, horasNormais, horasExtras, fixo, vendas, comissao};
    }

    public void rodaFolha(String data, String saida) throws Exception {
        saveState();
        DateTimeFormatter fmtEntrada = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter fmtSaida = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dataFolha = LocalDate.parse(data, fmtEntrada);

        List<Empregado> aPagar = empregados.values().stream()
                .filter(e -> ehDiaDePagamento(e, dataFolha))
                .sorted(Comparator.comparing(Empregado::getNome))
                .collect(Collectors.toList());
        
        Map<Empregado, Object[]> pagamentos = new LinkedHashMap<>();
        for(Empregado e : aPagar) {
            pagamentos.put(e, calcularPagamento(e, dataFolha));
        }

        try(PrintWriter writer = new PrintWriter(new FileWriter(saida))) {
            writer.println("FOLHA DE PAGAMENTO DO DIA " + dataFolha.format(fmtSaida));
            writer.println("====================================");

            gerarSecaoHoristas(writer, aPagar, pagamentos);
            gerarSecaoAssalariados(writer, aPagar, pagamentos);
            gerarSecaoComissionados(writer, aPagar, pagamentos);
            
            double totalFolha = aPagar.stream().mapToDouble(e -> (double)pagamentos.get(e)[0]).sum();
            writer.println();
            writer.printf("TOTAL FOLHA: %.2f\n", totalFolha);

            aPagar.forEach(e -> {
                 if((double) pagamentos.get(e)[0] > 0) {
                    e.setUltimoPagamento(dataFolha);
                }
            });
        }
        catch(Exception ex) {
            throw new Exception("Nao foi possivel salvar o arquivo.");
        }
    }

    private void gerarSecaoHoristas(PrintWriter writer, List<Empregado> empregadosAPagar, Map<Empregado, Object[]> pagamentos) {
        List<Empregado> horistas = empregadosAPagar.stream()
                .filter(e -> e instanceof EmpregadoHorista)
                .sorted(Comparator.comparing(Empregado::getNome))
                .collect(Collectors.toList());

        writer.println();
        writer.println("===============================================================================================================================");
        writer.println("===================== HORISTAS ================================================================================================");
        writer.println("===============================================================================================================================");
        writer.printf("%-36s %5s %5s %13s %9s %15s %s\n", "Nome", "Horas", "Extra", "Salario Bruto", "Descontos", "Salario Liquido", "Metodo");
        writer.println("==================================== ===== ===== ============= ========= =============== ======================================");

        double totalHoras = 0, totalExtra = 0, totalBruto = 0, totalDesc = 0, totalLiq = 0;
        if(!horistas.isEmpty()) {
            for(Empregado e : horistas) {
                Object[] p = pagamentos.get(e);
                double horasNormais = (double) p[3];
                double horasExtras = (double) p[4];

                totalHoras += horasNormais;
                totalExtra += horasExtras;
                totalBruto += (double) p[0];
                totalDesc += (double) p[1];
                totalLiq += (double) p[2];

                String metodo;
                if(e.getMetodoPagamento() instanceof Banco) {
                    metodo = ((Banco) e.getMetodoPagamento()).getDetalhes();
                }
                else if(e.getMetodoPagamento() instanceof EmMaos) {
                    metodo = ((EmMaos) e.getMetodoPagamento()).getDetalhes();
                }
                else {
                    metodo = "Correios, " + e.getEndereco();
                }
                writer.printf("%-36s %5.0f %5.0f %13s %9s %15s %s%n", e.getNome(), horasNormais, horasExtras, formatarValor((double) p[0]), formatarValor((double) p[1]), formatarValor((double) p[2]), metodo);
            }
        }
        writer.println();
        writer.printf("%-36s %5.0f %5.0f %13s %9s %15s\n", "TOTAL HORISTAS", totalHoras, totalExtra, formatarValor(totalBruto), formatarValor(totalDesc), formatarValor(totalLiq));
    }

    private void gerarSecaoAssalariados(PrintWriter writer, List<Empregado> empregadosAPagar, Map<Empregado, Object[]> pagamentos) {
        List<Empregado> assalariados = empregadosAPagar.stream()
                .filter(e -> e instanceof EmpregadoAssalariado && !(e instanceof EmpregadoComissionado))
                .sorted(Comparator.comparing(Empregado::getNome))
                .collect(Collectors.toList());

        writer.println();
        writer.println("===============================================================================================================================");
        writer.println("===================== ASSALARIADOS ============================================================================================");
        writer.println("===============================================================================================================================");

        String headerFmt = "%-48s %13s %9s %15s %s\n";
        String separator = "================================================ ============= ========= =============== ======================================";
        String totalFmt = "%-48s %13s %9s %15s\n";

        writer.printf(headerFmt, "Nome", "Salario Bruto", "Descontos", "Salario Liquido", "Metodo");
        writer.println(separator);

        double totalBruto = 0, totalDesc = 0, totalLiq = 0;
        if(!assalariados.isEmpty()) {
            for(Empregado e : assalariados) {
                Object[] p = pagamentos.get(e);
                totalBruto += (double) p[0];
                totalDesc += (double) p[1];
                totalLiq += (double) p[2];
                String metodo;
                if(e.getMetodoPagamento() instanceof Banco) {
                    metodo = ((Banco) e.getMetodoPagamento()).getDetalhes();
                }
                else if(e.getMetodoPagamento() instanceof EmMaos) {
                    metodo = ((EmMaos) e.getMetodoPagamento()).getDetalhes();
                }
                else {
                    metodo = "Correios, " + e.getEndereco();
                }
                writer.printf(headerFmt, e.getNome(), formatarValor((double) p[0]), formatarValor((double) p[1]), formatarValor((double) p[2]), metodo);
            }
        }
        writer.println();
        writer.printf(totalFmt, "TOTAL ASSALARIADOS", formatarValor(totalBruto), formatarValor(totalDesc), formatarValor(totalLiq));
    }

    private void gerarSecaoComissionados(PrintWriter writer, List<Empregado> empregadosAPagar, Map<Empregado, Object[]> pagamentos) {
        List<Empregado> comissionados = empregadosAPagar.stream()
                .filter(e -> e instanceof EmpregadoComissionado)
                .sorted(Comparator.comparing(Empregado::getNome))
                .collect(Collectors.toList());

        writer.println();
        writer.println("===============================================================================================================================");
        writer.println("===================== COMISSIONADOS ===========================================================================================");
        writer.println("===============================================================================================================================");

        String headerFmt = "%-17s %8s %10s %10s %13s %9s %15s %s\n";
        String separator = "===================== ======== ======== ======== ============= ========= =============== ======================================";
        String totalFmt = "%-19s %10.2f %8.2f %8.2f %13.2f %9.2f %15.2f\n";

        writer.printf(headerFmt, "Nome", "Fixo", "Vendas", "Comissao", "Salario Bruto", "Descontos", "Salario Liquido", "Metodo");
        writer.println(separator);

        double totalFixo = 0, totalVendas = 0, totalComissao = 0, totalBruto = 0, totalDesc = 0, totalLiq = 0;
        if(!comissionados.isEmpty()) {
            for(Empregado e : comissionados) {
                Object[] p = pagamentos.get(e);
                totalFixo += (double) p[5];
                totalVendas += (double) p[6];
                totalComissao += (double) p[7];
                totalBruto += (double) p[0];
                totalDesc += (double) p[1];
                totalLiq += (double) p[2];
                String metodo;
                if(e.getMetodoPagamento() instanceof Banco) {
                    metodo = ((Banco) e.getMetodoPagamento()).getDetalhes();
                }
                else if(e.getMetodoPagamento() instanceof EmMaos) {
                    metodo = ((EmMaos) e.getMetodoPagamento()).getDetalhes();
                }
                else {
                    metodo = "Correios, " + e.getEndereco();
                }
                writer.printf("%-21s %8.2f %8.2f %8.2f %13.2f %9.2f %15.2f %s\n", e.getNome(), (double) p[5], (double) p[6], (double) p[7], (double) p[0], (double) p[1], (double) p[2], metodo);
            }
        }
        writer.println();
        writer.printf(totalFmt, "TOTAL COMISSIONADOS", totalFixo, totalVendas, totalComissao, totalBruto, totalDesc, totalLiq);
    }
    
    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) throws WePayUException {
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
        saveState();
        this.empregados.put(novoId, novoEmpregado);
        return novoId;
    }

    public void removerEmpregado(String emp) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        if(!empregados.containsKey(emp)) {
            throw new EmpregadoNaoExisteException();
        }
        saveState();
        empregados.remove(emp);
    }
    public void alteraEmpregado(String emp, String atributo, String valor1, String valor2, String valor3) throws WePayUException, EmpregadoNaoExisteException {
        saveState();
        if(emp == null || emp.isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }

        switch (atributo.toLowerCase()) {
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
                else {
                    throw new MetodoPagamentoInvalidoException();
                }
                break;
            case "sindicalizado":
                if(valor1.equalsIgnoreCase("false")) {
                    e.setMembroSindicato(null);
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

                if(e instanceof EmpregadoHorista) {
                    ((EmpregadoHorista) e).setSalario(sal);
                }
                else if (e instanceof EmpregadoAssalariado) {
                    ((EmpregadoAssalariado) e).setSalario(sal);
                }
                else if (e instanceof EmpregadoComissionado) {
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

    public void alteraEmpregadoSindicalizado(String emp, String valor, String idSindicato, String taxaSindical) throws WePayUException, EmpregadoNaoExisteException {
        saveState();
        if(!valor.equalsIgnoreCase("true")) {
            return;
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(idSindicato == null || idSindicato.isEmpty()) {
            throw new IdentificacaoNulaException("sindicato");
        }

        for(Empregado outro : empregados.values()) {
            if(outro != e && outro.isSindicalizado() && outro.getMembroSindicato().getIdSindicato().equals(idSindicato)) {
                throw new WePayUException("Ha outro empregado com esta identificacao de sindicato");
            }
        }

        if(taxaSindical == null || taxaSindical.isEmpty()) {
            throw new TaxaSindicalInvalidaException("nao pode ser nula.");
        }

        double taxaNumerica;

        try {
            taxaNumerica = Double.parseDouble(taxaSindical.replace(",", "."));
        }
        catch(Exception ex) {
            throw new TaxaSindicalInvalidaException("deve ser numerica.");
        }
        if(taxaNumerica < 0) {
            throw new TaxaSindicalInvalidaException("deve ser nao-negativa.");
        }
        e.setMembroSindicato(new MembroSindicato(idSindicato, taxaNumerica));
    }

    public void alteraEmpregadoPagamentoBanco(String emp, String banco, String agencia, String contaCorrente) throws WePayUException, EmpregadoNaoExisteException {
        saveState();
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(banco == null || banco.isEmpty()) {
            throw new BancoNuloException();
        }
        if(agencia == null || agencia.isEmpty()) {
            throw new AgenciaNulaException();
        }
        if(contaCorrente == null || contaCorrente.isEmpty()) {
            throw new ContaCorrenteNulaException();
        }
        e.setMetodoPagamento(new Banco(banco, agencia, contaCorrente));
    }

    public String getAtributoEmpregado(String emp, String atributo) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }

        Empregado e = empregados.get(emp);

        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }

        switch (atributo.toLowerCase()) {
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
    
    public void lancaCartao(String emp, String data, String horas) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);

        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(!(e instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoHoristaException();
        }

        double horasNumericas;

        try {
            horasNumericas = Double.parseDouble(horas.replace(",", "."));
        }
        catch (NumberFormatException ex) {
            throw new HorasInvalidasException();
        }
        if(horasNumericas <= 0) throw new HorasInvalidasException();
        LocalDate dataConvertida;
        try {
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
            dataConvertida = LocalDate.parse(data, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("");
        }
        saveState();
        ((EmpregadoHorista) e).lancaCartao(new CartaoDePonto(dataConvertida, horasNumericas));
    }

    public String getHorasTrabalhadas(String emp, String dataInicial, String dataFinal, boolean saoExtras) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(!(e instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoHoristaException();
        }

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
        LocalDate dInicial, dFinal;
        try {
            dInicial = LocalDate.parse(dataInicial, formatador);
        }
        catch (DateTimeParseException ex) {
            throw new DataInvalidaException("inicial");
        }
        try {
            dFinal = LocalDate.parse(dataFinal, formatador);
        }
        catch (DateTimeParseException ex) {
            throw new DataInvalidaException("final");
        }
        if(dInicial.isAfter(dFinal)) {
            throw new WePayUException("Data inicial nao pode ser posterior aa data final.");
        }

        double totalHoras = 0;
        for(CartaoDePonto c : ((EmpregadoHorista) e).getCartoes()) {
            if(!c.getData().isBefore(dInicial) && c.getData().isBefore(dFinal)) {
                if(saoExtras) {
                    if(c.getHoras() > 8) {
                        totalHoras += c.getHoras() - 8;
                    }
                }
                else {
                    totalHoras += Math.min(c.getHoras(), 8);
                }
            }
        }

        if(totalHoras == 0) {
            return "0";
        }
        
        if(totalHoras % 1 == 0) {
            return String.format("%.0f", totalHoras);
        }
        return String.format("%.1f", totalHoras).replace('.', ',');
    }

    public void lancaVenda(String emp, String data, String valor) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(!(e instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoComissionadoException();
        }

        double valorNumerico;

        try {
            valorNumerico = Double.parseDouble(valor.replace(",", "."));
        }
        catch(NumberFormatException ex) {
            throw new ValorDeveSerPositivoException();
        }
        if(valorNumerico <= 0) {
            throw new ValorDeveSerPositivoException();
        }
        LocalDate dataConvertida;
        try {
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
            dataConvertida = LocalDate.parse(data, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("");
        }
        saveState();
        ((EmpregadoComissionado) e).lancaVenda(new ResultadoDeVenda(dataConvertida, valorNumerico));
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregados.get(emp);
        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(!(e instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoComissionadoException();
        }

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
        LocalDate dInicial, dFinal;
        try {
            dInicial = LocalDate.parse(dataInicial, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("inicial");
        }
        try {
            dFinal = LocalDate.parse(dataFinal, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("final");
        }

        if(dInicial.isAfter(dFinal)) {
            throw new WePayUException("Data inicial nao pode ser posterior aa data final.");
        }

        double totalVendas = 0;

        for(ResultadoDeVenda v : ((EmpregadoComissionado) e).getVendas()) {
            if(!v.getData().isBefore(dInicial) && v.getData().isBefore(dFinal)) {
                totalVendas += v.getValor();
            }
        }

        return String.format("%.2f", totalVendas).replace('.', ',');
    }

    private Empregado getEmpregadoPorMembro(String idMembro) {
        for (Empregado e : empregados.values()) {
            if(e.isSindicalizado() && e.getMembroSindicato().getIdSindicato().equals(idMembro)) {
                return e;
            }
        }
        return null;
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws WePayUException {
        if(membro == null || membro.trim().isEmpty()) {
            throw new IdentificacaoNulaException("membro");
        }

        Empregado e = getEmpregadoPorMembro(membro);

        if(e == null) {
            throw new MembroNaoExisteException();
        }

        double valorNumerico;

        try {
            valorNumerico = Double.parseDouble(valor.replace(",", "."));
        }
        catch(NumberFormatException ex) {
            throw new ValorDeveSerPositivoException();
        }
        if(valorNumerico <= 0) {
            throw new ValorDeveSerPositivoException();
        }
        
        LocalDate dataConvertida;
        try {
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
            dataConvertida = LocalDate.parse(data, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("");
        }
        saveState();
        e.getMembroSindicato().lancaTaxaServico(new TaxaServico(dataConvertida, valorNumerico));
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }

        Empregado e = empregados.get(emp);

        if(e == null) {
            throw new EmpregadoNaoExisteException();
        }
        if(!e.isSindicalizado()) {
            throw new EmpregadoNaoSindicalizadoException();
        }

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
        LocalDate dInicial, dFinal;
        try {
            dInicial = LocalDate.parse(dataInicial, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("inicial");
        }
        try {
            dFinal = LocalDate.parse(dataFinal, formatador);
        }
        catch(DateTimeParseException ex) {
            throw new DataInvalidaException("final");
        }

        if(dInicial.isAfter(dFinal)) {
            throw new WePayUException("Data inicial nao pode ser posterior aa data final.");
        }

        double totalTaxas = 0;

        for(TaxaServico t : e.getMembroSindicato().getTaxasDeServico()) {
            if(!t.getData().isBefore(dInicial) && t.getData().isBefore(dFinal)) {
                totalTaxas += t.getValor();
            }
        }

        return String.format("%.2f", totalTaxas).replace('.', ',');
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
    
    public int getNumeroDeEmpregados() {
        return empregados.size();
    }
}