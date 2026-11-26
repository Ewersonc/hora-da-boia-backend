package br.com.horadaboia.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação, no fuso de Brasília (RNF12).
 * Os services recebem o Clock por injeção, o que permite fixar a hora nos testes.
 */
@Configuration
public class TempoConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(FUSO);
    }
}
