package com.anderson.financialmanager.config;

import com.anderson.financialmanager.model.usuario.Usuario;
import com.anderson.financialmanager.model.usuario.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            String email = jwtService.extrairEmail(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
                if (usuario != null && jwtService.isTokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Invalid or expired JWT token; SecurityContext remains unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}
