package br.com.horadaboia.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import br.com.horadaboia.dto.AtendenteRequest;
import br.com.horadaboia.dto.CadastroRequest;
import br.com.horadaboia.exception.RecursoNaoEncontradoException;
import br.com.horadaboia.exception.RegraNegocioException;
import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.model.Usuario;
import br.com.horadaboia.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de clientes (RF01) e gestão de atendentes (RF04). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Usuario cadastrarCliente(CadastroRequest r) {
        return criar(r.nome(), r.email(), r.telefone(), r.senha(), Perfil.CLIENTE);
    }

    @Transactional
    public Usuario criarAtendente(AtendenteRequest r) {
        if (r.senha() == null || r.senha().isBlank()) {
            throw new RegraNegocioException("Informe a senha inicial do atendente.");
        }
        return criar(r.nome(), r.email(), r.telefone(), r.senha(), Perfil.ATENDENTE);
    }

    @Transactional
    public Usuario criarAdministrador(String nome, String email, String senha) {
        return criar(nome, email, null, senha, Perfil.ADMINISTRADOR);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarAtendentes() {
        return usuarioRepository.findByPerfilOrderByNomeAsc(Perfil.ATENDENTE);
    }

    @Transactional
    public Usuario atualizarAtendente(Long id, AtendenteRequest r) {
        Usuario atendente = buscarAtendente(id);
        String email = normalizar(r.email());
        if (!email.equals(atendente.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("E-mail já utilizado.");
        }
        atendente.setNome(r.nome().trim());
        atendente.setEmail(email);
        atendente.setTelefone(r.telefone());
        if (r.senha() != null && !r.senha().isBlank()) {
            atendente.setSenhaHash(passwordEncoder.encode(r.senha()));
        }
        if (r.ativo() != null) {
            atendente.setAtivo(r.ativo());
        }
        return atendente;
    }

    @Transactional
    public Usuario desativarAtendente(Long id) {
        Usuario atendente = buscarAtendente(id);
        atendente.setAtivo(false);
        return atendente;
    }

    private Usuario buscarAtendente(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getPerfil() == Perfil.ATENDENTE)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendente não encontrado."));
    }

    private Usuario criar(String nome, String email, String telefone, String senha, Perfil perfil) {
        String emailNormalizado = normalizar(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new RegraNegocioException("E-mail já utilizado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setTelefone(telefone);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setCriadoEm(LocalDateTime.now(clock));
        return usuarioRepository.save(usuario);
    }

    static String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
