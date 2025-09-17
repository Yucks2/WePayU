package br.ufal.ic.p2.wepayu.Services;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class LancamentoService {
    private EmpregadoService empregadoService;

    public LancamentoService(EmpregadoService empregadoService) {
        this.empregadoService = empregadoService;
    }
    
    public void lancaCartao(String emp, String data, String horas, CommandHistoryService commandHistory) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregadoService.getEmpregados().get(emp);

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
        catch(NumberFormatException ex) {
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
        ((EmpregadoHorista) e).lancaCartao(new CartaoDePonto(dataConvertida, horasNumericas));
    }

    public void lancaVenda(String emp, String data, String valor, CommandHistoryService commandHistory) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregadoService.getEmpregados().get(emp);
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

        ((EmpregadoComissionado) e).lancaVenda(new ResultadoDeVenda(dataConvertida, valorNumerico));
    }

    public void lancaTaxaServico(String membro, String data, String valor, CommandHistoryService commandHistory) throws WePayUException {
        if(membro == null || membro.trim().isEmpty()) {
            throw new IdentificacaoNulaException("membro");
        }

        Empregado e = null;
        for(Empregado emp : empregadoService.getEmpregados().values()){
            if(emp.isSindicalizado() && emp.getMembroSindicato().getIdSindicato().equals(membro)){
                e = emp;
                break;
            }
        }

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

        e.getMembroSindicato().lancaTaxaServico(new TaxaServico(dataConvertida, valorNumerico));
    }

    public String getHorasTrabalhadas(String emp, String dataInicial, String dataFinal, boolean saoExtras) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregadoService.getEmpregados().get(emp);
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

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }
        Empregado e = empregadoService.getEmpregados().get(emp);
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

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws WePayUException, EmpregadoNaoExisteException {
        if(emp == null || emp.trim().isEmpty()) {
            throw new IdentificacaoNulaException("empregado");
        }

        Empregado e = empregadoService.getEmpregados().get(emp);

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
}