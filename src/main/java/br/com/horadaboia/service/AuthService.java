package br.com.horadaboia.service;

import br.com.horadaboia.dto.AuthResponse;
import br.com.horadaboia.dto.CadastroRequest;
import br.com.horadaboia.dto.LoginRequest;
import br.com.horadaboia.exception.CredenciaisInvalidasException;
import br.com.horadaboia.model.Usuario;
import br.com.horadaboia.repository.UsuarioRepository;
import br.com.horadaboia.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login (RF02) e cadastro com entrada automática. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        Usuario usuario = usuarioRepository.findByEmail(UsuarioService.normalizar(r.email()))
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(r.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return resposta(usuario);
    }

    @Transactional
    public AuthResponse cadastrar(CadastroRequest r) {
        return resposta(usuarioService.cadastrarCliente(r));
    }

    private AuthResponse resposta(Usuario usuario) {
        return new AuthResponse(jwtService.gerar(usuario), usuario.getId(), usuario.getNome(), usuario.getPerfil());
    }
}
