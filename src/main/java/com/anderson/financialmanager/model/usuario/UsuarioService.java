package com.anderson.financialmanager.model.usuario;

import com.anderson.financialmanager.config.JwtService;
import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.usuario.dto.CadastroUsuarioRequest;
import com.anderson.financialmanager.model.usuario.dto.LoginRequest;
import com.anderson.financialmanager.model.usuario.dto.TokenResponse;
import com.anderson.financialmanager.model.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextHelper securityContextHelper;

    @Transactional
    public TokenResponse registrar(CadastroUsuarioRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com o e-mail informado.");
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .role(RoleUsuario.ROLE_USER)
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(novoUsuario);
        String token = jwtService.gerarToken(salvo);

        return new TokenResponse(
                token,
                "Bearer",
                jwtService.calcularDataExpiracao(),
                UsuarioResponse.fromEntity(salvo)
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticar(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailNormalizado, request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        String token = jwtService.gerarToken(usuario);
        return new TokenResponse(
                token,
                "Bearer",
                jwtService.calcularDataExpiracao(),
                UsuarioResponse.fromEntity(usuario)
        );
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obterPerfilAutenticado() {
        Usuario usuario = securityContextHelper.obterUsuarioAutenticado();
        return UsuarioResponse.fromEntity(usuario);
    }
}
