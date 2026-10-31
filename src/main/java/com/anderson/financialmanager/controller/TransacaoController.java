package com.anderson.financialmanager.controller;

import com.anderson.financialmanager.model.transacao.*;
import com.anderson.financialmanager.model.transacao.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;
    private final ExportacaoExtratoService exportacaoExtratoService;

    /**
     * Registra uma transação avulsa (Receita ou Despesa) vinculada à conta, categoria e usuário autenticado.
     */
    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(@Valid @RequestBody TransacaoRequest request) {
        TransacaoResponse criada = transacaoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    /**
     * Gera e persiste todas as parcelas futuras de uma compra/despesa parcelada.
     */
    @PostMapping("/parceladas")
    public ResponseEntity<ParcelamentoResponse> criarDespesaParcelada(
            @Valid @RequestBody DespesaParceladaRequest request
    ) {
        ParcelamentoResponse response = transacaoService.criarDespesaParcelada(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gera e persiste ocorrências de uma transação fixa/recorrente (ex: assinatura, aluguel, salário).
     */
    @PostMapping("/recorrentes")
    public ResponseEntity<ParcelamentoResponse> criarTransacaoRecorrente(
            @Valid @RequestBody TransacaoRecorrenteRequest request
    ) {
        ParcelamentoResponse response = transacaoService.criarTransacaoRecorrente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista transações do usuário autenticado com filtros opcionais de período, tipo, status, conta e categoria.
     */
    @GetMapping
    public ResponseEntity<List<TransacaoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) StatusTransacao status,
            @RequestParam(required = false) UUID contaId,
            @RequestParam(required = false) UUID categoriaId
    ) {
        return ResponseEntity.ok(
                transacaoService.listar(dataInicio, dataFim, tipo, status, contaId, categoriaId)
        );
    }

    /**
     * Busca uma transação específica pelo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransacaoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(transacaoService.buscarPorId(id));
    }

    /**
     * Lista todas as parcelas ou ocorrências pertencentes a um mesmo grupo de parcelamento/recorrência.
     */
    @GetMapping("/grupos/{grupoId}")
    public ResponseEntity<List<TransacaoResponse>> listarPorGrupo(@PathVariable UUID grupoId) {
        return ResponseEntity.ok(transacaoService.listarPorGrupo(grupoId));
    }

    /**
     * Cancela todas as parcelas/ocorrências ainda pendentes de um grupo de recorrência ou parcelamento.
     */
    @DeleteMapping("/grupos/{grupoId}/pendentes")
    public ResponseEntity<List<TransacaoResponse>> cancelarPendentesDoGrupo(@PathVariable UUID grupoId) {
        return ResponseEntity.ok(transacaoService.cancelarRecorrenciaOuParcelasFuturas(grupoId));
    }

    /**
     * Atualiza os dados de uma transação específica.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransacaoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TransacaoRequest request
    ) {
        return ResponseEntity.ok(transacaoService.atualizar(id, request));
    }

    /**
     * Efetiva (baixa/paga/recebe) uma transação pendente, definindo sua data de pagamento.
     */
    @PatchMapping("/{id}/efetivar")
    public ResponseEntity<TransacaoResponse> efetivar(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPagamento
    ) {
        return ResponseEntity.ok(transacaoService.efetivar(id, dataPagamento));
    }

    /**
     * Exclui permanentemente um lançamento específico.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        transacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint analítico que retorna o resumo financeiro mensal (entradas, saídas, saldo líquido e gastos por categoria).
     */
    @GetMapping("/resumo-mensal")
    public ResponseEntity<ResumoMensalResponse> obterResumoMensal(
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        return ResponseEntity.ok(transacaoService.obterResumoMensal(ano, mes));
    }

    /**
     * Exporta o extrato de transações filtrado por período e contas nos formatos CSV ou JSON (pretty-printed).
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarExtrato(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) List<UUID> contasIds,
            @RequestParam(defaultValue = "CSV") FormatoExportacao formato
    ) {
        byte[] conteudo = exportacaoExtratoService.exportarExtrato(dataInicio, dataFim, contasIds, formato);

        String extensao = formato == FormatoExportacao.CSV ? "csv" : "json";
        String nomeArquivo = "extrato_%s_a_%s.%s".formatted(dataInicio, dataFim, extensao);
        String contentType = formato == FormatoExportacao.CSV
                ? "text/csv;charset=UTF-8"
                : MediaType.APPLICATION_JSON_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(conteudo);
    }
}
