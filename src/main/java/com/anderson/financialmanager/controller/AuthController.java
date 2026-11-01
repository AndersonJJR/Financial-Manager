package com.anderson.financialmanager.controller;

import com.anderson.financialmanager.model.usuario.UsuarioService;
import com.anderson.financialmanager.model.usuario.dto.AtualizarPerfilRequest;
import com.anderson.financialmanager.model.usuario.dto.CadastroUsuarioRequest;
import com.anderson.financialmanager.model.usuario.dto.LoginRequest;
import com.anderson.financialmanager.model.usuario.dto.TokenResponse;
import com.anderson.financialmanager.model.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    /**
     * Registra um novo usuário e retorna o token JWT stateless.
     */
    @PostMapping("/registrar")
    public ResponseEntity<TokenResponse> registrar(@Valid @RequestBody CadastroUsuarioRequest request) {
        TokenResponse response = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica um usuário existente com e-mail e senha, retornando o token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.autenticar(request));
    }

    /**
     * Retorna os dados do usuário atualmente autenticado via SecurityContext.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obterPerfil() {
        return ResponseEntity.ok(usuarioService.obterPerfilAutenticado());
    }

    /**
     * Atualiza os dados cadastrais (nome) do usuário atualmente autenticado.
     */
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> atualizarPerfil(@Valid @RequestBody AtualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(request));
    }

    /**
     * Altera a senha do usuário atualmente autenticado.
     */
    @PutMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody com.anderson.financialmanager.model.usuario.dto.AlterarSenhaRequest request) {
        usuarioService.alterarSenha(request);
        return ResponseEntity.noContent().build();
    }
}
