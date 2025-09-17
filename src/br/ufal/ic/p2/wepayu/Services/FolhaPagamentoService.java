package br.ufal.ic.p2.wepayu.Services;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.Exception.WePayUException;
import br.ufal.ic.p2.wepayu.models.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FolhaPagamentoService {
    private EmpregadoService empregadoService;

    public FolhaPagamentoService(EmpregadoService empregadoService) {
        this.empregadoService = empregadoService;
    }
    
    public String totalFolha(String data) throws Exception {
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("d/M/yyyy");
        LocalDate dataFolha = LocalDate.parse(data, formatador);
        double total = 0;

        for(Empregado e : empregadoService.getEmpregados().values()) {
            if(ehDiaDePagamento(e, dataFolha)) {
                Object[] pagamento = calcularPagamento(e, dataFolha);
                total += (double)pagamento[0];
            }
        }
        return formatarValor(total);
    }

    public void rodaFolha(String data, String saida, CommandHistoryService commandHistory) throws Exception {
        DateTimeFormatter fmtEntrada = DateTimeFormatter.ofPattern("d/M/yyyy");
        DateTimeFormatter fmtSaida = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dataFolha = LocalDate.parse(data, fmtEntrada);

        List<Empregado> aPagar = empregadoService.getEmpregados().values().stream()
                .filter(e -> ehDiaDePagamento(e, dataFolha))
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
                 if((double) pagamentos.get(e)[0] - (double) pagamentos.get(e)[1] > 0) {
                    e.setUltimoPagamento(dataFolha);
                }
            });
        } catch(Exception ex) {
            throw new Exception("Nao foi possivel salvar o arquivo.");
        }
    }
    
    private boolean ehDiaDePagamento(Empregado e, LocalDate data) {
        if (e.getDataContratacao() != null && data.isBefore(e.getDataContratacao())) {
            return false;
        }

        String agenda = e.getAgendaPagamento();
        String[] partes = agenda.split(" ");
        String tipo = partes[0];

        if("mensal".equalsIgnoreCase(tipo)) {
            String dia = partes[1];
            if("$".equals(dia)) {
                LocalDate ultimoDiaUtil = data.with(TemporalAdjusters.lastDayOfMonth());
                while (ultimoDiaUtil.getDayOfWeek() == DayOfWeek.SATURDAY || ultimoDiaUtil.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    ultimoDiaUtil = ultimoDiaUtil.minusDays(1);
                }
                return data.equals(ultimoDiaUtil);
            }
            else {
                return data.getDayOfMonth() == Integer.parseInt(dia);
            }
        }
        else if("semanal".equalsIgnoreCase(tipo)) {
            int frequencia = partes.length == 2 ? 1 : Integer.parseInt(partes[1]);
            int diaSemana = Integer.parseInt(partes[partes.length - 1]);

            if(data.getDayOfWeek().getValue() != diaSemana) {
                return false;
            }

            if(e instanceof EmpregadoHorista && ((EmpregadoHorista) e).getDataContratacao() == null) return false;
            
            LocalDate dataAncora = (e.getDataContratacao() != null) ? e.getDataContratacao() : LocalDate.of(2005, 1, 1);
            LocalDate primeiroPagamento;

            if (frequencia == 2) {
                 primeiroPagamento = dataAncora.with(TemporalAdjusters.next(DayOfWeek.of(diaSemana))).with(TemporalAdjusters.next(DayOfWeek.of(diaSemana)));
            }
            else {
                 primeiroPagamento = dataAncora.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(diaSemana)));
            }

            if (data.isBefore(primeiroPagamento)) return false;

            long semanasDesdeAncora = ChronoUnit.WEEKS.between(primeiroPagamento, data);
            return semanasDesdeAncora % frequencia == 0;
        }
        return false;
    }

    private String formatarValor(double valor) {
        return String.format("%.2f", valor).replace('.', ',');
    }

    private Object[] calcularPagamento(Empregado e, LocalDate dataFim) throws EmpregadoNaoExisteException, WePayUException {
        LocalDate dataInicio = (e.getUltimoPagamento() == null) ? dataFim.withDayOfYear(1).minusDays(1) : e.getUltimoPagamento();
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
            salarioBruto = Math.floor((salarioBruto * 100) + 1e-9) / 100.0;
        }
        else if(e instanceof EmpregadoComissionado) {
            EmpregadoComissionado c = (EmpregadoComissionado) e;
            fixo = (c.getSalario() * 12) / 26.0;
            fixo = Math.floor(fixo * 100) / 100.0;
            double total_comissao = 0;
            for(ResultadoDeVenda v : c.getVendas()) {
                if(!v.getData().isBefore(dataInicio) && !v.getData().isAfter(dataFim)) {
                    vendas += v.getValor();
                    total_comissao += v.getValor() * c.getTaxaDeComissao();
                }
            }
            comissao = Math.floor(total_comissao * 100) / 100.0;
            salarioBruto = fixo + comissao;
        }
        else if(e instanceof EmpregadoAssalariado) {
            salarioBruto = e.getSalario();
        }

        if(e.isSindicalizado()) {
            MembroSindicato membro = e.getMembroSindicato();
            double taxaSindicalDiaria = membro.getTaxaSindical();
            double taxaSindicalTotal = 0;
            
            LocalDate inicioDesconto = (e.getUltimoPagamento() == null) ? dataFim.withDayOfYear(1) : e.getUltimoPagamento().plusDays(1);

            if(e instanceof EmpregadoAssalariado){
                taxaSindicalTotal = taxaSindicalDiaria * dataFim.lengthOfMonth();
            } else{
                long dias = ChronoUnit.DAYS.between(inicioDesconto, dataFim) + 1;
                taxaSindicalTotal = dias * taxaSindicalDiaria;
            }

            descontos += taxaSindicalTotal;

            for(TaxaServico t : membro.getTaxasDeServico()) {
                if(!t.getData().isBefore(inicioDesconto) && !t.getData().isAfter(dataFim)) {
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

        String headerFmt = "%-21s %8s %8s %8s %13s %9s %15s %s\n";
        String separator = "===================== ======== ======== ======== ============= ========= =============== ======================================";
        String totalFmt = "%-19s %10s %8s %8s %13s %9s %15s\n";

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
                writer.printf(headerFmt, e.getNome(), formatarValor((double) p[5]), formatarValor((double) p[6]), formatarValor((double) p[7]), formatarValor((double) p[0]), formatarValor((double) p[1]), formatarValor((double) p[2]), metodo);
            }
        }
        writer.println();
        writer.printf(totalFmt, "TOTAL COMISSIONADOS", formatarValor(totalFixo), formatarValor(totalVendas), formatarValor(totalComissao), formatarValor(totalBruto), formatarValor(totalDesc), formatarValor(totalLiq));
    }
}