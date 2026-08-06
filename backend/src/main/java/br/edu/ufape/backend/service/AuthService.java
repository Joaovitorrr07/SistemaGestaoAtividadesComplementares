package br.edu.ufape.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.ufape.backend.dto.CadastroUsuarioRequest;
import br.edu.ufape.backend.dto.LoginRequest;
import br.edu.ufape.backend.dto.LoginResponse;
import br.edu.ufape.backend.exception.UnauthorizedException;
import br.edu.ufape.backend.model.Usuario;
import br.edu.ufape.backend.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UsuarioService usuarioService,
                       UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       TokenBlacklistService tokenBlacklistService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public Usuario cadastrarUsuario(CadastroUsuarioRequest request) {
        return usuarioService.cadastrar(request.getNome(), request.getEmail(), request.getSenha(), request.getRole());
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getUsuario())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(usuario.getEmail());
        return new LoginResponse(token, "Bearer");
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return;
        }

        tokenBlacklistService.blacklistToken(token);
    }
}
