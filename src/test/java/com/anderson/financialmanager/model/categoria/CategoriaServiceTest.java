package com.anderson.financialmanager.model.categoria;

import com.anderson.financialmanager.config.SecurityContextHelper;
import com.anderson.financialmanager.exception.RegraDeNegocioException;
import com.anderson.financialmanager.model.categoria.dto.CategoriaRequest;
import com.anderson.financialmanager.model.categoria.dto.CategoriaResponse;
import com.anderson.financialmanager.model.transacao.TipoTransacao;
import com.anderson.financialmanager.model.usuario.RoleUsuario;
import com.anderson.financialmanager.model.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CategoriaService categoriaService;

    private Usuario usuarioPadrao;
    private Categoria categoriaAtiva;
    private Categoria categoriaInativa;

    @BeforeEach
    void setUp() {
        usuarioPadrao = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Anderson")
                .email("anderson@email.com")
                .role(RoleUsuario.ROLE_USER)
                .ativo(true)
                .build();

        categoriaAtiva = Categoria.builder()
                .id(UUID.randomUUID())
                .nome("Alimentação")
                .tipo(TipoTransacao.DESPESA)
                .corHex("#FF5733")
                .ativa(true)
                .usuario(usuarioPadrao)
                .build();

        categoriaInativa = Categoria.builder()
                .id(UUID.randomUUID())
                .nome("Lazer")
                .tipo(TipoTransacao.DESPESA)
                .corHex("#33FF57")
                .ativa(false)
                .usuario(usuarioPadrao)
                .build();
    }

    @Test
    @DisplayName("Deve criar nova categoria quando não existir duplicidade")
    void deveCriarCategoriaComSucesso() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioPadrao.getId(), "Transporte", TipoTransacao.DESPESA))
                .thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> {
            Categoria c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoriaRequest request = new CategoriaRequest("Transporte", "Combustível", "#000000", TipoTransacao.DESPESA, true);
        CategoriaResponse response = categoriaService.criar(request);

        assertNotNull(response);
        assertEquals("Transporte", response.nome());
        assertEquals(TipoTransacao.DESPESA, response.tipo());
        assertTrue(response.ativa());
    }

    @Test
    @DisplayName("Deve reativar categoria inativa existente ao criar com mesmo nome e tipo")
    void deveReativarCategoriaInativaExistente() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioPadrao.getId(), "Lazer", TipoTransacao.DESPESA))
                .thenReturn(Optional.of(categoriaInativa));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaRequest request = new CategoriaRequest("Lazer", "Viagens e cinema", "#00FF00", TipoTransacao.DESPESA, true);
        CategoriaResponse response = categoriaService.criar(request);

        assertNotNull(response);
        assertTrue(response.ativa());
        assertEquals("Viagens e cinema", response.descricao());
        assertEquals("#00FF00", response.corHex());
    }

    @Test
    @DisplayName("Deve rejeitar criação quando categoria ativa com mesmo nome e tipo já existir")
    void deveRejeitarQuandoCategoriaAtivaJaExistir() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioPadrao.getId(), "Alimentação", TipoTransacao.DESPESA))
                .thenReturn(Optional.of(categoriaAtiva));

        CategoriaRequest request = new CategoriaRequest("Alimentação", "Comida", "#FF0000", TipoTransacao.DESPESA, true);

        assertThrows(RegraDeNegocioException.class, () -> categoriaService.criar(request));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve filtrar categorias por tipo e status ativo")
    void deveListarCategoriasFiltrandoPorTipoEStatus() {
        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuarioPadrao);
        when(categoriaRepository.findAllByUsuarioIdAndTipoAndAtivaOrderByNomeAsc(usuarioPadrao.getId(), TipoTransacao.DESPESA, true))
                .thenReturn(List.of(categoriaAtiva));

        List<CategoriaResponse> responses = categoriaService.listarTodas(TipoTransacao.DESPESA, true);

        assertEquals(1, responses.size());
        assertEquals("Alimentação", responses.getFirst().nome());
    }
}
