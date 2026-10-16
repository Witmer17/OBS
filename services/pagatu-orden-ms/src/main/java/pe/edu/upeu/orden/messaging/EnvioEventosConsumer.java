package pe.edu.upeu.orden.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.orden.event.EnvioEvento;
import pe.edu.upeu.orden.service.OrdenService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioEventosConsumer {

    private static final String ENVIO_PROGRAMADO = "envio.programado";
    private static final String ENVIO_FALLIDO = "envio.fallido";

    private final OrdenService ordenService;

    @KafkaListener(topics = "${app.kafka.topic.envios:envio-eventos}")
    public void alRecibirEnvio(EnvioEvento evento) {
        switch (evento.getTipoEvento()) {
            case ENVIO_PROGRAMADO -> {
                log.info("component=consumer eventType={} ordenId={} status=consumed",
                        evento.getTipoEvento(), evento.getOrdenId());
                ordenService.marcarDespachada(evento.getOrdenId());
            }
            case ENVIO_FALLIDO -> {
                log.info("component=consumer eventType={} ordenId={} status=consumed",
                        evento.getTipoEvento(), evento.getOrdenId());
                ordenService.compensarEnvio(evento.getOrdenId());
            }
            default -> log.warn("component=consumer eventType={} status=ignored", evento.getTipoEvento());
        }
    }
}

