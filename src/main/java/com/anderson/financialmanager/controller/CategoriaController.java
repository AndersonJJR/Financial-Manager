package com.anderson.financialmanager.controller;

import com.anderson.financialmanager.model.categoria.CategoriaService;
import com.anderson.financialmanager.model.categoria.dto.CategoriaRequest;
import com.anderson.financialmanager.model.categoria.dto.CategoriaResponse;
import com.anderson.financialmanager.model.transacao.TipoTransacao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Cria uma nova categoria de receita ou despesa vinculada ao usuário autenticado.
     */
    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse criada = categoriaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    /**
     * Lista as categorias do usuário autenticado, com filtros opcionais por tipo (RECEITA/DESPESA) e status ativo/inativo.
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarTodas(
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) Boolean ativa
    ) {
        return ResponseEntity.ok(categoriaService.listarTodas(tipo, ativa));
    }

    /**
     * Busca uma categoria específica pelo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    /**
     * Atualiza uma categoria existente do usuário autenticado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CategoriaRequest request
    ) {
        return ResponseEntity.ok(categoriaService.atualizar(id, request));
    }

    /**
     * Inativa logicamente uma categoria do usuário autenticado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
