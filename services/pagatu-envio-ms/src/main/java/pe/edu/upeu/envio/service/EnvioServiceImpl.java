package pe.edu.upeu.envio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.envio.dto.EnvioResponse;
import pe.edu.upeu.envio.entity.Envio;
import pe.edu.upeu.envio.entity.EstadoEnvio;
import pe.edu.upeu.envio.event.DespachoSolicitadoEvento;
import pe.edu.upeu.envio.event.EnvioEvento;
import pe.edu.upeu.envio.messaging.EnvioEventosProducer;
import pe.edu.upeu.envio.repository.EnvioRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements EnvioService {

    private static final String ENVIO_PROGRAMADO = "envio.programado";
    private static final String ENVIO_FALLIDO = "envio.fallido";
    private static final String ZONA_RECHAZADA = "ZONA_NO_COBERTURA";

    private final EnvioRepository envioRepository;
    private final EnvioEventosProducer producer;

    @Value("${spring.application.name}")
    private String nombreServicio;

    @Override
    @Transactional
    public void procesarDespacho(DespachoSolicitadoEvento evento) {
        // Capa 1 de Idempotencia: Verificacion explicita previa en BD antes de actuar
        if (envioRepository.findByOrdenId(evento.getOrdenId()).isPresent()) {
            log.warn("component=processor ordenId={} status=ignored motivo=\"envio ya procesado (evento duplicado)\"",
                    evento.getOrdenId());
            return;
        }

        // Condicion de fallo real: Direccion sin cobertura
        boolean sinCobertura = evento.getDireccionEnvio() != null &&
                (evento.getDireccionEnvio().toUpperCase().contains(ZONA_RECHAZADA)
                 || evento.getDireccionEnvio().equalsIgnoreCase(ZONA_RECHAZADA));

        EstadoEnvio estadoResultante = sinCobertura ? EstadoEnvio.RECHAZADO : EstadoEnvio.PROGRAMADO;
        String guia = sinCobertura ? null : "GUIA-" + evento.getOrdenId() + "-" + System.currentTimeMillis();
        String motivo = sinCobertura ? "Sin cobertura para la direccion indicada: " + evento.getDireccionEnvio() : null;

        // Capa 2 de Idempotencia: Restriccion UNIQUE 'uk_envios_orden' en la base de datos contra condicion de carrera
        Envio envio;
        try {
            envio = envioRepository.save(Envio.builder()
                    .ordenId(evento.getOrdenId())
                    .direccionEnvio(evento.getDireccionEnvio())
                    .estado(estadoResultante)
                    .guiaSeguimiento(guia)
                    .motivoRechazo(motivo)
                    .fechaCreacion(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.warn("component=processor ordenId={} status=ignored motivo=\"envio ya procesado (condicion de carrera)\"",
                    evento.getOrdenId());
            return;
        }

        // Publicacion del evento de resultado (exito o compensacion saga)
        String tipoEventoResultante = sinCobertura ? ENVIO_FALLIDO : ENVIO_PROGRAMADO;
        producer.publicarTrasCommit(EnvioEvento.builder()
                .tipoEvento(tipoEventoResultante)
                .ordenId(envio.getOrdenId())
                .estado(envio.getEstado().name())
                .guiaSeguimiento(envio.getGuiaSeguimiento())
                .motivo(envio.getMotivoRechazo())
                .origen(nombreServicio)
                .timestamp(Instant.now().toEpochMilli())
                .build());

        log.info("component=processor ordenId={} estado={} status=processed",
                envio.getOrdenId(), envio.getEstado());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvioResponse> listar() {
        return envioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorOrdenId(Long ordenId) {
        return envioRepository.findByOrdenId(ordenId)
                .map(this::toResponse)
                .orElse(null);
    }

    private EnvioResponse toResponse(Envio envio) {
        return EnvioResponse.builder()
                .id(envio.getId())
                .ordenId(envio.getOrdenId())
                .direccionEnvio(envio.getDireccionEnvio())
                .estado(envio.getEstado())
                .guiaSeguimiento(envio.getGuiaSeguimiento())
                .motivoRechazo(envio.getMotivoRechazo())
                .fechaCreacion(envio.getFechaCreacion())
                .build();
    }
}

