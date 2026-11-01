package com.anderson.financialmanager.model.transacao;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.conta.ContaService;
import com.anderson.financialmanager.model.transacao.dto.ExtratoExportacaoDto;
import com.anderson.financialmanager.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportacaoExtratoService {

    private static final String CSV_DELIMITER = ";";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final TransacaoRepository transacaoRepository;
    private final ContaService contaService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public byte[] exportarExtrato(
            LocalDate dataInicio,
            LocalDate dataFim,
            List<UUID> contasIds,
            FormatoExportacao formato
    ) {
        validarPeriodo(dataInicio, dataFim);
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();

        List<Transacao> transacoes;
        if (contasIds != null && !contasIds.isEmpty()) {
            contasIds.forEach(contaId -> contaService.buscarEntidadeDoUsuario(contaId, usuario.getId()));
            transacoes = transacaoRepository.findAllByUsuarioIdAndPeriodoAndContas(
                    usuario.getId(),
                    dataInicio,
                    dataFim,
                    contasIds
            );
        } else {
            transacoes = transacaoRepository.findAllByUsuarioIdAndPeriodo(
                    usuario.getId(),
                    dataInicio,
                    dataFim
            );
        }

        ExtratoExportacaoDto extratoDto = montarExtratoDto(dataInicio, dataFim, transacoes);

        return switch (formato) {
            case CSV -> gerarCsv(extratoDto);
            case JSON -> gerarJsonPrettyPrinted(extratoDto);
        };
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new RegraDeNegocioException("As datas inicial e final são obrigatórias para exportação do extrato.");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser posterior à data final.");
        }
    }

    private ExtratoExportacaoDto montarExtratoDto(LocalDate dataInicio, LocalDate dataFim, List<Transacao> transacoes) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);

        BigDecimal totalReceitas = transacoes.stream()
                .filter(t -> t.getStatus() != StatusTransacao.CANCELADA && t.getTipo() == TipoTransacao.RECEITA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDespesas = transacoes.stream()
                .filter(t -> t.getStatus() != StatusTransacao.CANCELADA && t.getTipo() == TipoTransacao.DESPESA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldoPeriodo = totalReceitas.subtract(totalDespesas);

        List<ExtratoExportacaoDto.ItemExtratoDto> itens = transacoes.stream()
                .map(t -> new ExtratoExportacaoDto.ItemExtratoDto(
                        t.getId(),
                        t.getDataCompetencia(),
                        t.getDataPagamento(),
                        t.getDescricao(),
                        t.getTipo().name(),
                        t.getStatus().name(),
                        t.getModalidade().name(),
                        t.getReferenciaParcela() != null ? t.getReferenciaParcela() : "-",
                        t.getValor(),
                        currencyFormatter.format(t.getValor()),
                        t.getConta().getNome(),
                        t.getConta().getInstituicao() != null ? t.getConta().getInstituicao() : "-",
                        t.getCategoria().getNome(),
                        t.getObservacao() != null ? t.getObservacao() : ""
                ))
                .toList();

        return new ExtratoExportacaoDto(
                dataInicio,
                dataFim,
                LocalDateTime.now(),
                itens.size(),
                totalReceitas,
                totalDespesas,
                saldoPeriodo,
                itens
        );
    }

    private byte[] gerarCsv(ExtratoExportacaoDto extrato) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.join(CSV_DELIMITER,
                "ID",
                "Data Competencia",
                "Data Pagamento",
                "Descricao",
                "Tipo",
                "Status",
                "Modalidade",
                "Parcela",
                "Valor Numerico",
                "Valor Formatado (BRL)",
                "Conta",
                "Instituicao",
                "Categoria",
                "Observacao"
        )).append("\r\n");

        for (ExtratoExportacaoDto.ItemExtratoDto item : extrato.transacoes()) {
            sb.append(escaparCsv(item.id().toString())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.dataCompetencia().format(DATE_FORMATTER))).append(CSV_DELIMITER)
                    .append(escaparCsv(item.dataPagamento() != null ? item.dataPagamento().format(DATE_FORMATTER) : "-")).append(CSV_DELIMITER)
                    .append(escaparCsv(item.descricao())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.tipo())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.status())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.modalidade())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.parcela())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.valor().toPlainString().replace('.', ','))).append(CSV_DELIMITER)
                    .append(escaparCsv(item.valorFormatado())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.conta())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.instituicaoConta())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.categoria())).append(CSV_DELIMITER)
                    .append(escaparCsv(item.observacao()))
                    .append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String limpo = valor.replace("\r", " ").replace("\n", " ");
        // Mitigação de CSV Formula Injection (CWE-1236)
        if (!limpo.isEmpty() && (limpo.startsWith("=") || limpo.startsWith("+") || limpo.startsWith("-") || limpo.startsWith("@") || limpo.startsWith("\t"))) {
            limpo = "'" + limpo;
        }
        if (limpo.contains(CSV_DELIMITER) || limpo.contains("\"")) {
            return "\"" + limpo.replace("\"", "\"\"") + "\"";
        }
        return limpo;
    }

    private byte[] gerarJsonPrettyPrinted(ExtratoExportacaoDto extrato) {
        return JSON_MAPPER.writeValueAsBytes(extrato);
    }
}
