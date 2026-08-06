package br.edu.ufape.backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ufape.backend.exception.EmailJaCadastradoException;
import br.edu.ufape.backend.exception.PerfilNaoPermitidoException;
import br.edu.ufape.backend.model.Role;
import br.edu.ufape.backend.model.Usuario;
import br.edu.ufape.backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Usuario cadastrar(String nome, String email, String senhaTexto, Role role) {
        if (usuarioRepository.existsByEmail(email)){
            throw new EmailJaCadastradoException(email);
        }

        if (role != null && role != Role.ESTUDANTE) {
            throw new PerfilNaoPermitidoException();
        }

        String senhaHash = passwordEncoder.encode(senhaTexto);
        Usuario usuario = new Usuario(nome, email, senhaHash, Role.ESTUDANTE);

        return usuarioRepository.save(usuario);
    }
    
}
