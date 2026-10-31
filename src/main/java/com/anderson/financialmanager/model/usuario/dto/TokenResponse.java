package com.anderson.financialmanager.model.usuario.dto;

import java.time.Instant;

public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario
) {
}
