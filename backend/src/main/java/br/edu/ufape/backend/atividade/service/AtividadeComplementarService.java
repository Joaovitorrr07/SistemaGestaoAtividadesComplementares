package br.edu.ufape.backend.atividade.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufape.backend.atividade.dto.AtividadeResponseDTO;
import br.edu.ufape.backend.atividade.dto.AtualizarAtividadeRequestDTO;
import br.edu.ufape.backend.atividade.dto.CadastroAtividadeRequestDTO;
import br.edu.ufape.backend.atividade.exception.AcessoNegadoAtividadeException;
import br.edu.ufape.backend.atividade.exception.AtividadeNaoEncontradaException;
import br.edu.ufape.backend.atividade.model.AtividadeComplementar;
import br.edu.ufape.backend.atividade.model.Categoria;
import br.edu.ufape.backend.atividade.model.Natureza;
import br.edu.ufape.backend.atividade.repository.AtividadeComplementarRepository;
import br.edu.ufape.backend.certificados.exception.CertificadoInvalidoException;
import br.edu.ufape.backend.certificados.model.Certificado;
import br.edu.ufape.backend.certificados.service.ArmazenamentoCertificadoService;
import br.edu.ufape.backend.usuario.contrato.UsuarioContrato;
import br.edu.ufape.backend.usuario.model.Estudante;
import br.edu.ufape.backend.usuario.model.Usuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class AtividadeComplementarService {

    private static final String MENSAGEM_ACESSO_NEGADO = "Apenas estudantes podem listar atividades complementares.";
    private final Validator validator;

    /*
     * Mesma mensagem/exceção usada tanto para "atividade não existe" quanto para
     * "atividade pertence a outro estudante". É proposital: distinguir os dois
     * casos permitiria a um usuário mal-intencionado descobrir quais IDs existem
     * no banco só testando exclusões (enumeração de recursos).
     */
    private static final String MENSAGEM_ACESSO_NEGADO_ATIVIDADE = "Atividade não encontrada ou não pertence ao estudante autenticado.";  // mantido, mas deve ser removido
    private static final String MENSAGEM_ACESSO_NEGADO_EDICAO = "Você não tem permissão para editar esta atividade.";
    private static final String MENSAGEM_ACESSO_NEGADO_EXCLUSAO = "Atividade não encontrada ou não pertence ao estudante autenticado.";

    private final AtividadeComplementarRepository atividadeRepository;
    private final UsuarioContrato usuarioContrato;
    private final ArmazenamentoCertificadoService armazenamentoCertificadoService;

    public AtividadeComplementarService(
            AtividadeComplementarRepository atividadeRepository,
            UsuarioContrato usuarioContrato,
            ArmazenamentoCertificadoService armazenamentoCertificadoService,
            Validator validator) {
        this.atividadeRepository = atividadeRepository;
        this.usuarioContrato = usuarioContrato;
        this.armazenamentoCertificadoService = armazenamentoCertificadoService;
        this.validator = validator;
    }

    public List<AtividadeComplementar> listarAtividadesDoEstudante(
            String emailEstudante, Natureza natureza, Categoria categoria) {
        Estudante estudante = obterEstudante(emailEstudante);
        return atividadeRepository.findByEstudanteComFiltros(estudante, natureza, categoria);
    }

    public Resource obterArquivoCertificado(Long id, String emailEstudante) {
        Estudante estudante = obterEstudante(emailEstudante);
        AtividadeComplementar atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new AtividadeNaoEncontradaException("Atividade não encontrada."));

        if (!atividade.getEstudante().getId().equals(estudante.getId())) {
            throw new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO_EDICAO);
        }

        Certificado certificado = atividade.getCertificado();
        if (certificado == null || certificado.getReferencia() == null) {
            throw new AtividadeNaoEncontradaException("Certificado não encontrado para esta atividade.");
        }

        try {
            Path caminho = Paths.get(certificado.getReferencia());
            Resource resource = new UrlResource(caminho.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AtividadeNaoEncontradaException("Arquivo físico do certificado não encontrado no servidor.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao recuperar arquivo do certificado", e);
        }
    }

    @Transactional
    public AtividadeResponseDTO cadastrarAtividade(CadastroAtividadeRequestDTO request, MultipartFile arquivo,
            String emailEstudante) {
        validarTipoArquivo(arquivo);

        Usuario estudante = usuarioContrato.buscarPorEmail(emailEstudante)
                .orElseThrow(() -> new RuntimeException("Estudante não encontrado"));

        Certificado certificado = armazenamentoCertificadoService.armazenar(arquivo);

        AtividadeComplementar atividade = new AtividadeComplementar(
                request.titulo(),
                request.instituicaoResponsavel(),
                request.dataRealizacao(),
                request.cargaHoraria(),
                request.natureza(),
                request.categoria(),
                certificado,
                estudante);

        try {
            AtividadeComplementar atividadeSalva = atividadeRepository.save(atividade);
            return new AtividadeResponseDTO(atividadeSalva);
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(Paths.get(certificado.getReferencia()));
            } catch (IOException ioException) {
                e.addSuppressed(ioException);
            }
            throw e;
        }
    }

    @Transactional
    public AtividadeResponseDTO atualizarAtividade(Long id, AtualizarAtividadeRequestDTO request,
            MultipartFile novoArquivo, String emailEstudante) {
        Estudante estudante = obterEstudante(emailEstudante);

        AtividadeComplementar atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new AtividadeNaoEncontradaException("Atividade não encontrada."));

        if (!atividade.getEstudante().getId().equals(estudante.getId())) {
            throw new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO_EDICAO);
        }

        atividade.setTitulo(request.titulo());
        atividade.setInstituicaoResponsavel(request.instituicaoResponsavel());
        atividade.setDataRealizacao(request.dataRealizacao());
        atividade.setCargaHorariaEmHoras(request.cargaHoraria());
        atividade.setNatureza(request.natureza());
        atividade.setCategoria(request.categoria());

        Certificado certificadoAntigo = atividade.getCertificado();
        Certificado novoCertificado = null;

        if (novoArquivo != null && !novoArquivo.isEmpty()) {
            validarTipoArquivo(novoArquivo);
            novoCertificado = armazenamentoCertificadoService.armazenar(novoArquivo);
            atividade.setCertificado(novoCertificado);
        }

        try {
            AtividadeComplementar atividadeSalva = atividadeRepository.save(atividade);

            if (novoCertificado != null && certificadoAntigo != null) {
                removerArquivoCertificado(certificadoAntigo);
            }

            return new AtividadeResponseDTO(atividadeSalva);
        } catch (RuntimeException e) {
            if (novoCertificado != null) {
                removerArquivoCertificado(novoCertificado);
            }
            throw e;
        }
    }

    @Transactional
    public void excluirAtividade(Long id, String emailEstudante) {
        Estudante estudante = obterEstudante(emailEstudante);

        AtividadeComplementar atividade = atividadeRepository.findByIdAndEstudante(id, estudante)
                .orElseThrow(() -> new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO_ATIVIDADE));

        removerArquivoCertificado(atividade.getCertificado());

        atividadeRepository.delete(atividade);
    }

    @Transactional
    public AtividadeResponseDTO atualizarAtividade(Long id, CadastroAtividadeRequestDTO request, String emailEstudante) {
        validarDados(request);

        Estudante estudante = obterEstudante(emailEstudante);

        AtividadeComplementar atividade = atividadeRepository.findByIdAndEstudante(id, estudante)
                .orElseThrow(() -> new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO_ATIVIDADE));

        atividade.setTitulo(request.titulo());
        atividade.setInstituicaoResponsavel(request.instituicaoResponsavel());
        atividade.setDataRealizacao(request.dataRealizacao());
        atividade.setCargaHorariaEmHoras(request.cargaHoraria());
        atividade.setNatureza(request.natureza());
        atividade.setCategoria(request.categoria());

        AtividadeComplementar atividadeAtualizada = atividadeRepository.save(atividade);
        return new AtividadeResponseDTO(atividadeAtualizada);
    }

    private void validarDados(CadastroAtividadeRequestDTO request) {
        Set<ConstraintViolation<CadastroAtividadeRequestDTO>> violacoes = validator.validate(request);
        if (!violacoes.isEmpty()) {
            throw new ConstraintViolationException(violacoes);
        }
    }

    private void removerArquivoCertificado(Certificado certificado) {
        if (certificado == null || certificado.getReferencia() == null) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(certificado.getReferencia()));
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao remover arquivo do certificado", ex);
        }
    }

    private Estudante obterEstudante(String email) {
        Usuario usuario = usuarioContrato.buscarPorEmail(email)
                .orElseThrow(() -> new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO));

        if (!(usuario instanceof Estudante estudante)) {
            throw new AcessoNegadoAtividadeException(MENSAGEM_ACESSO_NEGADO);
        }

        return estudante;
    }

    private void validarTipoArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new CertificadoInvalidoException("Arquivo de certificado não pode ser vazio");
        }

        String tipo = arquivo.getContentType();
        if (tipo == null || !(tipo.equals("application/pdf") || tipo.equals("image/png") || tipo.equals("image/jpeg")
                || tipo.equals("image/jpg"))) {
            throw new CertificadoInvalidoException("Certificado inválido. Aceitos: PDF, PNG ou JPEG");
        }
    }
}