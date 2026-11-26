package br.com.horadaboia.scheduler;

import br.com.horadaboia.service.ReservaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verifica a cada minuto as reservas que passaram da tolerância de atraso (RF17, RN07).
 * Pode ser desligado com app.scheduler.habilitado=false (os testes fazem isso para controlar o relógio).
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.habilitado", havingValue = "true", matchIfMissing = true)
public class NaoComparecimentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(NaoComparecimentoScheduler.class);

    private final ReservaService reservaService;

    public NaoComparecimentoScheduler(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void verificar() {
        try {
            int atualizadas = reservaService.marcarNaoComparecimento();
            if (atualizadas > 0) {
                log.info("{} reserva(s) marcada(s) como não compareceu.", atualizadas);
            }
        } catch (RuntimeException ex) {
            log.error("Falha ao marcar não comparecimento.", ex);
        }
    }
}
