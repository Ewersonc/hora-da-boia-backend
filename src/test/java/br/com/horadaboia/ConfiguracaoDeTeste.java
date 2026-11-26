package br.com.horadaboia;

import java.time.Instant;
import java.time.ZonedDateTime;

import br.com.horadaboia.config.TempoConfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Substitui o relógio da aplicação por um relógio controlável: segunda-feira, 16/11/2026, 17:00 em Brasília. */
@TestConfiguration
public class ConfiguracaoDeTeste {

    public static final Instant INICIO = ZonedDateTime.of(2026, 11, 16, 17, 0, 0, 0, TempoConfig.FUSO).toInstant();

    @Bean
    @Primary
    public RelogioMutavel relogioDeTeste() {
        return new RelogioMutavel(INICIO, TempoConfig.FUSO);
    }
}
