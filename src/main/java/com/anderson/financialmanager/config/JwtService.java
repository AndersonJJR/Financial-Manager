package com.anderson.financialmanager.config;

import com.anderson.financialmanager.model.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${security.jwt.issuer:financial-manager-api}") String issuer
    ) {
        this.signingKey = buildSecretKey(secret);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    private SecretKey buildSecretKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length >= 32) {
                return Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (IllegalArgumentException ignored) {
            // fallback to raw UTF-8 bytes
        }
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(rawBytes, 0, padded, 0, rawBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(rawBytes);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plusMillis(expirationMs);

        return Jwts.builder()
                .issuer(issuer)
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId().toString())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Instant calcularDataExpiracao() {
        return Instant.now().plusMillis(expirationMs);
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public UUID extrairUsuarioId(String token) {
        String userId = extrairClaims(token).get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    public boolean isTokenValido(String token, Usuario usuario) {
        try {
            Claims claims = extrairClaims(token);
            String email = claims.getSubject();
            Date expiration = claims.getExpiration();
            return email != null
                    && email.equalsIgnoreCase(usuario.getEmail())
                    && expiration != null
                    && expiration.after(new Date())
                    && Boolean.TRUE.equals(usuario.getAtivo());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
