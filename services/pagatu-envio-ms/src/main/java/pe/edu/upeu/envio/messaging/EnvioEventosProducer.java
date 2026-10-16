package pe.edu.upeu.envio.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.edu.upeu.envio.event.EnvioEvento;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioEventosProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.envios}")
    private String topicEnvios;

    public void publicarTrasCommit(EnvioEvento evento) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(evento);
                }
            });
        } else {
            enviar(evento);
        }
    }

    private void enviar(EnvioEvento evento) {
        String key = String.valueOf(evento.getOrdenId());
        kafkaTemplate.send(topicEnvios, key, evento)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("component=producer topic={} key={} eventType={} status=published",
                                topicEnvios, key, evento.getTipoEvento());
                    } else {
                        log.error("component=producer topic={} key={} eventType={} status=failed error={}",
                                topicEnvios, key, evento.getTipoEvento(), ex.getMessage());
                    }
                });
    }
}

