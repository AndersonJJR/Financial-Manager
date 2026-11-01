package com.anderson.financialmanager.model.usuario;

import com.anderson.financialmanager.config.JwtService;
import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.model.usuario.dto.AtualizarPerfilRequest;
import com.anderson.financialmanager.model.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioPadrao;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuarioPadrao = Usuario.builder()
                .id(usuarioId)
                .nome("Mariana Silva")
                .email("mariana@email.com")
                .senhaHash("hash-seguro")
                .role(RoleUsuario.ROLE_USER)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar os dados do usuário autenticado no SecurityContext")
    void deveObterPerfilAutenticado() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);

        UsuarioResponse response = usuarioService.obterPerfilAutenticado();

        assertNotNull(response);
        assertEquals(usuarioId, response.id());
        assertEquals("Mariana Silva", response.nome());
        assertEquals("mariana@email.com", response.email());
        assertEquals(RoleUsuario.ROLE_USER, response.role());
        assertTrue(response.ativo());
    }

    @Test
    @DisplayName("Deve atualizar o nome do perfil com sucesso e retornar UsuarioResponse atualizado")
    void deveAtualizarPerfilComSucesso() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarPerfilRequest request = new AtualizarPerfilRequest("Mariana Silva Atualizada");
        UsuarioResponse response = usuarioService.atualizarPerfil(request);

        assertNotNull(response);
        assertEquals("Mariana Silva Atualizada", response.nome());
        verify(usuarioRepository, times(1)).save(usuarioPadrao);
    }

    @Test
    @DisplayName("Deve alterar a senha com sucesso quando a senha atual estiver correta")
    void deveAlterarSenhaComSucesso() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("hash-seguro", usuarioPadrao.getSenhaHash())).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novo-hash-seguro");

        com.anderson.financialmanager.model.usuario.dto.AlterarSenhaRequest request =
                new com.anderson.financialmanager.model.usuario.dto.AlterarSenhaRequest("hash-seguro", "novaSenha123");

        usuarioService.alterarSenha(request);

        assertEquals("novo-hash-seguro", usuarioPadrao.getSenhaHash());
        verify(usuarioRepository, times(1)).save(usuarioPadrao);
    }

    @Test
    @DisplayName("Deve rejeitar alteração de senha quando senha atual estiver incorreta")
    void deveRejeitarAlteracaoDeSenhaComSenhaIncorreta() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(passwordEncoder.matches("senha-errada", usuarioPadrao.getSenhaHash())).thenReturn(false);

        com.anderson.financialmanager.model.usuario.dto.AlterarSenhaRequest request =
                new com.anderson.financialmanager.model.usuario.dto.AlterarSenhaRequest("senha-errada", "novaSenha123");

        assertThrows(
                com.anderson.financialmanager.exception.RegraDeNegocioException.class,
                () -> usuarioService.alterarSenha(request)
        );
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
