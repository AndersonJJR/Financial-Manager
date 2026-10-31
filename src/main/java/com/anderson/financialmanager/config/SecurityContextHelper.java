package com.anderson.financialmanager.config;

import com.anderson.financialmanager.exception.AcessoNegadoException;
import com.anderson.financialmanager.model.usuario.Usuario;
import com.anderson.financialmanager.model.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UsuarioRepository usuarioRepository;

    public Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AcessoNegadoException("Usuário não autenticado no contexto de segurança.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        if (principal instanceof UserDetails userDetails) {
            return usuarioRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .orElseThrow(() -> new AcessoNegadoException("Usuário autenticado não encontrado."));
        }
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return usuarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new AcessoNegadoException("Usuário autenticado não encontrado."));
        }

        throw new AcessoNegadoException("Não foi possível identificar o usuário autenticado.");
    }
}
