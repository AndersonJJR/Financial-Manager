package com.anderson.financialmanager.controller;

import com.anderson.financialmanager.model.conta.ContaService;
import com.anderson.financialmanager.model.conta.dto.ContaRequest;
import com.anderson.financialmanager.model.conta.dto.ContaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contas")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

    /**
     * Cria uma nova conta financeira (banco, carteira, corretora, etc.) vinculada ao usuário autenticado.
     */
    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody ContaRequest request) {
        ContaResponse criada = contaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    /**
     * Lista as contas pertencentes ao usuário autenticado, com filtro opcional por status ativo/inativo.
     */
    @GetMapping
    public ResponseEntity<List<ContaResponse>> listarTodas(
            @RequestParam(required = false) Boolean ativa
    ) {
        return ResponseEntity.ok(contaService.listarTodas(ativa));
    }

    /**
     * Busca uma conta específica pelo ID (restrita ao usuário autenticado).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(contaService.buscarPorId(id));
    }

    /**
     * Atualiza os dados cadastrais de uma conta financeira.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ContaRequest request
    ) {
        return ResponseEntity.ok(contaService.atualizar(id, request));
    }

    /**
     * Inativa logicamente uma conta financeira do usuário autenticado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        contaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
