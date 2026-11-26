package br.com.horadaboia.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Gera e valida os tokens JWT (RF02, RNF03). O relógio injetado define a emissão e a expiração. */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;
    private final Clock clock;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos,
                      Clock clock) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
        this.clock = clock;
    }

    public String gerar(Usuario usuario) {
        Instant agora = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            Perfil perfil = Perfil.valueOf(claims.get("perfil", String.class));
            return Optional.of(new UsuarioAutenticado(id, email, perfil));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
