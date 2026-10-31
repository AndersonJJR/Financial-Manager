package com.anderson.financialmanager.model.usuario.dto;

import com.anderson.financialmanager.model.usuario.RoleUsuario;
import com.anderson.financialmanager.model.usuario.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        RoleUsuario role,
        Boolean ativo,
        LocalDateTime criadoEm
) {
    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getCriadoEm()
        );
    }
}
