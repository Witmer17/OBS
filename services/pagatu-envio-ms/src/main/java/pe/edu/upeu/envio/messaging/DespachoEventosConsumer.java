package pe.edu.upeu.envio.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.envio.event.DespachoSolicitadoEvento;
import pe.edu.upeu.envio.service.EnvioService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DespachoEventosConsumer {

    private static final String DESPACHO_SOLICITADO = "despacho.solicitado";

    private final EnvioService envioService;

    @KafkaListener(topics = "${app.kafka.topic.despachos}")
    public void alRecibirDespacho(DespachoSolicitadoEvento evento) {
        if (!DESPACHO_SOLICITADO.equals(evento.getTipoEvento())) {
            log.warn("component=consumer eventType={} status=ignored", evento.getTipoEvento());
            return;
        }
        log.info("component=consumer eventType={} ordenId={} status=consumed",
                evento.getTipoEvento(), evento.getOrdenId());
        envioService.procesarDespacho(evento);
    }
}

