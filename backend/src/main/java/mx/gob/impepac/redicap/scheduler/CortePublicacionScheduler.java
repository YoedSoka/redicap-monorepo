package mx.gob.impepac.redicap.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.impepac.redicap.service.PublicacionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta el corte de publicación cada 10 minutos.
 * La cron se configura en application.yml: redicap.scheduler.corte-cron
 */
@Component @RequiredArgsConstructor @Slf4j
public class CortePublicacionScheduler {

    private final PublicacionService publicacionService;

    @Scheduled(cron = "${redicap.scheduler.corte-cron}")
    public void ejecutarCorte() {
        log.info("Iniciando corte de publicación...");
        publicacionService.generarCorte();
        log.info("Corte de publicación completado");
    }
}
