package br.edu.ufape.backend.atividadeTest.unidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import br.edu.ufape.backend.atividade.dto.AtividadeResponseDTO;
import br.edu.ufape.backend.atividade.dto.AtualizarAtividadeRequestDTO;
import br.edu.ufape.backend.atividade.dto.CadastroAtividadeRequestDTO;
import br.edu.ufape.backend.atividade.exception.AcessoNegadoAtividadeException;
import br.edu.ufape.backend.atividade.exception.AtividadeNaoEncontradaException;
import br.edu.ufape.backend.atividade.model.AtividadeComplementar;
import br.edu.ufape.backend.atividade.model.Categoria;
import br.edu.ufape.backend.atividade.model.Natureza;
import br.edu.ufape.backend.atividade.repository.AtividadeComplementarRepository;
import br.edu.ufape.backend.atividade.service.AtividadeComplementarService;
import br.edu.ufape.backend.certificados.exception.CertificadoInvalidoException;
import br.edu.ufape.backend.certificados.model.Certificado;
import br.edu.ufape.backend.certificados.service.ArmazenamentoCertificadoService;
import br.edu.ufape.backend.usuario.contrato.UsuarioContrato;
import br.edu.ufape.backend.usuario.model.Avaliador;
import br.edu.ufape.backend.usuario.model.Estudante;

@ExtendWith(MockitoExtension.class)
class AtividadeComplementarServiceTest {

        private static final String EMAIL = "estudante@ufape.edu.br";
        private static final Long ID_ATIVIDADE = 1L;

        @TempDir
        Path tempDir;

        @Mock
        private UsuarioContrato usuarioContrato;

        @Mock
        private AtividadeComplementarRepository atividadeRepository;

        @Mock
        private ArmazenamentoCertificadoService armazenamentoCertificadoService;

        @InjectMocks
        private AtividadeComplementarService service;

        private AtividadeComplementar criarAtividade(Natureza natureza, Categoria categoria, Estudante estudante) {
                return new AtividadeComplementar(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                natureza,
                                categoria,
                                null,
                                estudante);
        }

        private AtividadeComplementar criarAtividadeComCertificado(Estudante estudante, String referencia) {
                Certificado certificado = new Certificado("certificado.pdf", "application/pdf", 100L, referencia);
                return new AtividadeComplementar(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA,
                                certificado,
                                estudante);
        }

        @Test
        @DisplayName("Estudante sem atividades retorna lista vazia")
        void estudanteSemAtividadesRetornaListaVazia() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByEstudanteComFiltros(estudante, null, null)).thenReturn(List.of());

                List<AtividadeComplementar> resultado = service.listarAtividadesDoEstudante(EMAIL, null, null);

                assertTrue(resultado.isEmpty());
                verify(atividadeRepository).findByEstudanteComFiltros(estudante, null, null);
        }

        @Test
        @DisplayName("Estudante com atividades retorna apenas as atividades dele")
        void estudanteComAtividadesRetornaApenasAtividadesDele() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                AtividadeComplementar atividade1 = criarAtividade(Natureza.ACC, Categoria.PESQUISA, estudante);
                AtividadeComplementar atividade2 = criarAtividade(Natureza.ACEX, Categoria.EXTENSAO, estudante);
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByEstudanteComFiltros(estudante, null, null))
                                .thenReturn(List.of(atividade1, atividade2));

                List<AtividadeComplementar> resultado = service.listarAtividadesDoEstudante(EMAIL, null, null);

                assertEquals(2, resultado.size());
                verify(atividadeRepository).findByEstudanteComFiltros(estudante, null, null);
        }

        @Test
        @DisplayName("Filtro apenas por Natureza funciona corretamente")
        void filtroApenasPorNaturezaFuncionaCorretamente() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                AtividadeComplementar atividadeAcc = criarAtividade(Natureza.ACC, Categoria.PESQUISA, estudante);
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByEstudanteComFiltros(estudante, Natureza.ACC, null))
                                .thenReturn(List.of(atividadeAcc));

                List<AtividadeComplementar> resultado = service.listarAtividadesDoEstudante(EMAIL, Natureza.ACC, null);

                assertEquals(1, resultado.size());
                assertEquals(Natureza.ACC, resultado.get(0).getNatureza());
                verify(atividadeRepository).findByEstudanteComFiltros(estudante, Natureza.ACC, null);
        }

        @Test
        @DisplayName("Filtro por Natureza e Categoria funciona corretamente")
        void filtroPorNaturezaECategoriaFuncionaCorretamente() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                AtividadeComplementar atividade = criarAtividade(Natureza.ACC, Categoria.PESQUISA, estudante);
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByEstudanteComFiltros(estudante, Natureza.ACC, Categoria.PESQUISA))
                                .thenReturn(List.of(atividade));

                List<AtividadeComplementar> resultado = service.listarAtividadesDoEstudante(
                                EMAIL, Natureza.ACC, Categoria.PESQUISA);

                assertEquals(1, resultado.size());
                assertEquals(Natureza.ACC, resultado.get(0).getNatureza());
                assertEquals(Categoria.PESQUISA, resultado.get(0).getCategoria());
                verify(atividadeRepository).findByEstudanteComFiltros(estudante, Natureza.ACC, Categoria.PESQUISA);
        }

        @Test
        @DisplayName("Usuario avaliador lanca AcessoNegadoAtividadeException")
        void usuarioAvaliadorLancaAcessoNegadoAtividadeException() {
                Avaliador avaliador = new Avaliador("Avaliador", EMAIL, "hash", "REG-1", "Extensao");
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(avaliador));

                assertThrows(
                                AcessoNegadoAtividadeException.class,
                                () -> service.listarAtividadesDoEstudante(EMAIL, null, null));
        }

        @Test
        @DisplayName("E-mail inexistente lanca AcessoNegadoAtividadeException")
        void emailInexistenteLancaAcessoNegadoAtividadeException() {
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

                assertThrows(
                                AcessoNegadoAtividadeException.class,
                                () -> service.listarAtividadesDoEstudante(EMAIL, null, null));
        }

        @Test
        @DisplayName("Deve excluir atividade do proprio estudante removendo entidade e arquivo")
        void deveExcluirAtividadeDoProprioEstudanteRemovendoEntidadeEArquivo() throws IOException {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                Path arquivoCertificado = tempDir.resolve("certificado.pdf");
                Files.createFile(arquivoCertificado);

                AtividadeComplementar atividade = criarAtividadeComCertificado(estudante,
                                arquivoCertificado.toString());

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByIdAndEstudante(ID_ATIVIDADE, estudante))
                                .thenReturn(Optional.of(atividade));

                service.excluirAtividade(ID_ATIVIDADE, EMAIL);

                assertTrue(Files.notExists(arquivoCertificado), "O arquivo do certificado deveria ter sido removido");
                verify(atividadeRepository).delete(atividade);
        }

        @Test
        @DisplayName("Exclusao de atividade que nao pertence ao estudante lanca acesso negado")
        void exclusaoDeAtividadeDeOutroEstudanteLancaAcessoNegado() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByIdAndEstudante(ID_ATIVIDADE, estudante)).thenReturn(Optional.empty());

                assertThrows(
                                AcessoNegadoAtividadeException.class,
                                () -> service.excluirAtividade(ID_ATIVIDADE, EMAIL));
                verify(atividadeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Exclusao com id inexistente lanca erro apropriado")
        void exclusaoComIdInexistenteLancaErroApropriado() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                Long idInexistente = 9999L;
                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByIdAndEstudante(idInexistente, estudante)).thenReturn(Optional.empty());

                assertThrows(
                                AcessoNegadoAtividadeException.class,
                                () -> service.excluirAtividade(idInexistente, EMAIL));
                verify(atividadeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Falha ao remover arquivo do certificado nao deixa o banco inconsistente")
        void falhaAoRemoverArquivoNaoDeixaBancoInconsistente() throws IOException {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                Path diretorioNaoVazio = tempDir.resolve("certificados-com-falha");
                Files.createDirectory(diretorioNaoVazio);
                Files.createFile(diretorioNaoVazio.resolve("arquivo-interno.txt"));

                AtividadeComplementar atividade = criarAtividadeComCertificado(estudante, diretorioNaoVazio.toString());

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findByIdAndEstudante(ID_ATIVIDADE, estudante))
                                .thenReturn(Optional.of(atividade));

                assertThrows(
                                RuntimeException.class,
                                () -> service.excluirAtividade(ID_ATIVIDADE, EMAIL));
                verify(atividadeRepository, never()).delete(any());
                assertFalse(Files.notExists(diretorioNaoVazio), "O diretorio nao deveria ter sido removido");
        }

        @Test
        @DisplayName("Falha ao salvar atividade remove o certificado gravado em disco (rollback manual)")
        void falhaAoSalvarAtividadeRemoveCertificadoGravadoEmDisco() throws IOException {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                Path arquivoCertificado = tempDir.resolve("certificado.pdf");
                Files.createFile(arquivoCertificado);
                Certificado certificado = new Certificado(
                                "certificado.pdf", "application/pdf", 100L, arquivoCertificado.toString());

                MockMultipartFile arquivo = new MockMultipartFile(
                                "arquivo", "certificado.pdf", "application/pdf", "conteudo".getBytes());
                CadastroAtividadeRequestDTO request = new CadastroAtividadeRequestDTO(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(armazenamentoCertificadoService.armazenar(arquivo)).thenReturn(certificado);
                when(atividadeRepository.save(any())).thenThrow(new RuntimeException("Falha ao persistir atividade"));

                assertThrows(
                                RuntimeException.class,
                                () -> service.cadastrarAtividade(request, arquivo, EMAIL));

                assertTrue(Files.notExists(arquivoCertificado),
                                "O certificado gravado em disco deveria ter sido removido apos falha no cadastro");
                verify(armazenamentoCertificadoService).armazenar(arquivo);
        }

        @Test
        @DisplayName("Cadastro de atividade com dados validos retorna atividade salva (caminho feliz)")
        void cadastroDeAtividadeComDadosValidosRetornaAtividadeSalva() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                Certificado certificado = new Certificado("certificado.pdf", "application/pdf", 100L,
                                "/tmp/certificado.pdf");
                MockMultipartFile arquivo = new MockMultipartFile(
                                "arquivo", "certificado.pdf", "application/pdf", "conteudo".getBytes());
                CadastroAtividadeRequestDTO request = new CadastroAtividadeRequestDTO(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA);
                AtividadeComplementar atividadeSalva = new AtividadeComplementar(
                                request.titulo(),
                                request.instituicaoResponsavel(),
                                request.dataRealizacao(),
                                request.cargaHoraria(),
                                request.natureza(),
                                request.categoria(),
                                certificado,
                                estudante);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(armazenamentoCertificadoService.armazenar(arquivo)).thenReturn(certificado);
                when(atividadeRepository.save(any())).thenReturn(atividadeSalva);

                AtividadeResponseDTO resposta = service.cadastrarAtividade(request, arquivo, EMAIL);

                assertEquals(request.titulo(), resposta.titulo());
                assertEquals(request.natureza(), resposta.natureza());
                assertEquals(request.categoria(), resposta.categoria());
                assertEquals(EMAIL, resposta.estudanteEmail());
                verify(atividadeRepository).save(any());
        }

        @Test
        @DisplayName("Cadastro com estudante inexistente lanca RuntimeException e nao grava certificado")
        void cadastroComEstudanteInexistenteLancaRuntimeException() {
                MockMultipartFile arquivo = new MockMultipartFile(
                                "arquivo", "certificado.pdf", "application/pdf", "conteudo".getBytes());
                CadastroAtividadeRequestDTO request = new CadastroAtividadeRequestDTO(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

                assertThrows(
                                RuntimeException.class,
                                () -> service.cadastrarAtividade(request, arquivo, EMAIL));
                verify(armazenamentoCertificadoService, never()).armazenar(any());
                verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cadastro com arquivo vazio lanca CertificadoInvalidoException")
        void cadastroComArquivoVazioLancaCertificadoInvalidoException() {
                MockMultipartFile arquivoVazio = new MockMultipartFile(
                                "arquivo", "certificado.pdf", "application/pdf", new byte[0]);
                CadastroAtividadeRequestDTO request = new CadastroAtividadeRequestDTO(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                assertThrows(
                                CertificadoInvalidoException.class,
                                () -> service.cadastrarAtividade(request, arquivoVazio, EMAIL));
                verify(usuarioContrato, never()).buscarPorEmail(any());
                verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cadastro com tipo de arquivo nao suportado lanca CertificadoInvalidoException")
        void cadastroComTipoDeArquivoNaoSuportadoLancaCertificadoInvalidoException() {
                MockMultipartFile arquivoInvalido = new MockMultipartFile(
                                "arquivo", "certificado.txt", "text/plain", "conteudo".getBytes());
                CadastroAtividadeRequestDTO request = new CadastroAtividadeRequestDTO(
                                "Atividade de teste",
                                "Instituicao",
                                LocalDate.now(),
                                10,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                assertThrows(
                                CertificadoInvalidoException.class,
                                () -> service.cadastrarAtividade(request, arquivoInvalido, EMAIL));
                verify(usuarioContrato, never()).buscarPorEmail(any());
                verify(atividadeRepository, never()).save(any());
        }

        /* ---------------------------------------------------------------------- */
        /* Testes de Atualização e Substituição de Certificado (SGAC-ATV-03a/03b) */
        /* ---------------------------------------------------------------------- */

        @Test
        @DisplayName("Edicao do proprio registro sem novo arquivo atualiza campos e mantem certificado original")
        void edicaoDoProprioRegistroSemNovoArquivoAtualizaCamposEMantemCertificadoOriginal() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                estudante.setId(1L);
                AtividadeComplementar atividadeOriginal = criarAtividadeComCertificado(estudante,
                                "/tmp/certificado_antigo.pdf");
                atividadeOriginal.setId(ID_ATIVIDADE);

                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo Atualizado",
                                "Nova Instituicao",
                                LocalDate.of(2026, 6, 1),
                                40,
                                Natureza.ACEX,
                                Categoria.EXTENSAO);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findById(ID_ATIVIDADE)).thenReturn(Optional.of(atividadeOriginal));
                when(atividadeRepository.save(any(AtividadeComplementar.class))).thenAnswer(i -> i.getArgument(0));

                AtividadeResponseDTO resposta = service.atualizarAtividade(ID_ATIVIDADE, request, null, EMAIL);

                assertEquals("Titulo Atualizado", resposta.titulo());
                assertEquals("Nova Instituicao", resposta.instituicaoResponsavel());
                assertEquals(LocalDate.of(2026, 6, 1), resposta.dataRealizacao());
                assertEquals(40, resposta.cargaHorariaEmHoras());
                assertEquals(Natureza.ACEX, resposta.natureza());
                assertEquals(Categoria.EXTENSAO, resposta.categoria());
                verify(armazenamentoCertificadoService, never()).armazenar(any());
                verify(atividadeRepository).save(atividadeOriginal);
        }

        @Test
        @DisplayName("Edicao com novo arquivo valido substitui o certificado e remove o arquivo antigo do disco")
        void edicaoComNovoArquivoValidoSubstituiCertificadoERemoveArquivoAntigoDoDisco() throws IOException {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                estudante.setId(1L);

                Path arquivoAntigo = tempDir.resolve("certificado_antigo.pdf");
                Files.createFile(arquivoAntigo);

                AtividadeComplementar atividadeOriginal = criarAtividadeComCertificado(estudante,
                                arquivoAntigo.toString());
                atividadeOriginal.setId(ID_ATIVIDADE);

                MockMultipartFile novoArquivo = new MockMultipartFile(
                                "arquivo", "novo_certificado.pdf", "application/pdf", "conteudo".getBytes());
                Certificado novoCertificado = new Certificado(
                                "novo_certificado.pdf", "application/pdf", 200L,
                                tempDir.resolve("novo_certificado.pdf").toString());

                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo Atualizado",
                                "Instituicao",
                                LocalDate.now(),
                                20,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findById(ID_ATIVIDADE)).thenReturn(Optional.of(atividadeOriginal));
                when(armazenamentoCertificadoService.armazenar(novoArquivo)).thenReturn(novoCertificado);
                when(atividadeRepository.save(any(AtividadeComplementar.class))).thenAnswer(i -> i.getArgument(0));

                AtividadeResponseDTO resposta = service.atualizarAtividade(ID_ATIVIDADE, request, novoArquivo, EMAIL);

                assertNotNull(resposta);
                assertEquals(novoCertificado, atividadeOriginal.getCertificado());
                assertTrue(Files.notExists(arquivoAntigo),
                                "O arquivo antigo em disco deveria ter sido removido apos o sucesso");
                verify(armazenamentoCertificadoService).armazenar(novoArquivo);
                verify(atividadeRepository).save(atividadeOriginal);
        }

        @Test
        @DisplayName("Edicao com arquivo invalido lanca CertificadoInvalidoException sem alterar nem persistir")
        void edicaoComArquivoInvalidoLancaCertificadoInvalidoException() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                estudante.setId(1L);
                AtividadeComplementar atividadeOriginal = criarAtividadeComCertificado(estudante, "/tmp/antigo.pdf");
                atividadeOriginal.setId(ID_ATIVIDADE);

                MockMultipartFile arquivoInvalido = new MockMultipartFile(
                                "arquivo", "documento.exe", "application/x-msdownload", "conteudo".getBytes());

                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo",
                                "Instituicao",
                                LocalDate.now(),
                                20,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findById(ID_ATIVIDADE)).thenReturn(Optional.of(atividadeOriginal));

                assertThrows(
                                CertificadoInvalidoException.class,
                                () -> service.atualizarAtividade(ID_ATIVIDADE, request, arquivoInvalido, EMAIL));

                verify(armazenamentoCertificadoService, never()).armazenar(any());
                verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edicao de atividade pertencente a outro estudante lanca AcessoNegadoAtividadeException")
        void edicaoDeAtividadeDeOutroEstudanteLancaAcessoNegado() {
                Estudante estudanteAutenticado = new Estudante("Estudante 1", EMAIL, "hash");
                estudanteAutenticado.setId(1L);

                Estudante outroEstudante = new Estudante("Estudante 2", "outro@ufape.edu.br", "hash");
                outroEstudante.setId(2L);

                AtividadeComplementar atividadeDeOutro = criarAtividade(Natureza.ACC, Categoria.PESQUISA,
                                outroEstudante);
                atividadeDeOutro.setId(ID_ATIVIDADE);

                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo",
                                "Instituicao",
                                LocalDate.now(),
                                20,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudanteAutenticado));
                when(atividadeRepository.findById(ID_ATIVIDADE)).thenReturn(Optional.of(atividadeDeOutro));

                assertThrows(
                                AcessoNegadoAtividadeException.class,
                                () -> service.atualizarAtividade(ID_ATIVIDADE, request, null, EMAIL));

                verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edicao com id inexistente lanca AtividadeNaoEncontradaException")
        void edicaoComIdInexistenteLancaAtividadeNaoEncontradaException() {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                estudante.setId(1L);
                Long idInexistente = 9999L;

                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo",
                                "Instituicao",
                                LocalDate.now(),
                                20,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findById(idInexistente)).thenReturn(Optional.empty());

                assertThrows(
                                AtividadeNaoEncontradaException.class,
                                () -> service.atualizarAtividade(idInexistente, request, null, EMAIL));

                verify(atividadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falha ao salvar edicao remove o novo certificado gravado em disco (compensacao)")
        void falhaAoSalvarEdicaoRemoveNovoCertificadoGravadoEmDisco() throws IOException {
                Estudante estudante = new Estudante("Estudante", EMAIL, "hash");
                estudante.setId(1L);

                Path arquivoAntigo = tempDir.resolve("antigo.pdf");
                Files.createFile(arquivoAntigo);
                AtividadeComplementar atividade = criarAtividadeComCertificado(estudante, arquivoAntigo.toString());
                atividade.setId(ID_ATIVIDADE);

                Path novoArquivoPath = tempDir.resolve("novo.pdf");
                Files.createFile(novoArquivoPath);
                Certificado novoCertificado = new Certificado(
                                "novo.pdf", "application/pdf", 100L, novoArquivoPath.toString());

                MockMultipartFile arquivo = new MockMultipartFile(
                                "arquivo", "novo.pdf", "application/pdf", "conteudo".getBytes());
                AtualizarAtividadeRequestDTO request = new AtualizarAtividadeRequestDTO(
                                "Titulo",
                                "Instituicao",
                                LocalDate.now(),
                                20,
                                Natureza.ACC,
                                Categoria.PESQUISA);

                when(usuarioContrato.buscarPorEmail(EMAIL)).thenReturn(Optional.of(estudante));
                when(atividadeRepository.findById(ID_ATIVIDADE)).thenReturn(Optional.of(atividade));
                when(armazenamentoCertificadoService.armazenar(arquivo)).thenReturn(novoCertificado);
                when(atividadeRepository.save(any())).thenThrow(new RuntimeException("Falha de banco de dados"));

                assertThrows(
                                RuntimeException.class,
                                () -> service.atualizarAtividade(ID_ATIVIDADE, request, arquivo, EMAIL));

                assertTrue(Files.notExists(novoArquivoPath),
                                "O novo arquivo gravado deveria ter sido removido na compensacao");
                assertTrue(Files.exists(arquivoAntigo), "O arquivo antigo deve ser preservado se a transacao falhar");
        }
}