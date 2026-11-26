package br.com.horadaboia.config;

import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.repository.UsuarioRepository;
import br.com.horadaboia.service.UsuarioService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador inicial na primeira execução, com e-mail e senha vindos de variáveis de
 * ambiente (ADMIN_EMAIL e ADMIN_PASSWORD). A senha nunca fica no repositório.
 */
@Component
public class AdministradorInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicializador.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final String email;
    private final String senha;

    public AdministradorInicializador(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
                                      @Value("${app.admin.email:}") String email,
                                      @Value("${app.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Nenhum administrador existe e ADMIN_EMAIL/ADMIN_PASSWORD não foram definidos.");
            return;
        }
        usuarioService.criarAdministrador("Administrador", email, senha);
        log.info("Administrador inicial criado com o e-mail {}.", email);
    }
}
