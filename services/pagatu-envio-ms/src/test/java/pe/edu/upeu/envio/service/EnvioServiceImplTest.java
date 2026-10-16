package pe.edu.upeu.envio.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import pe.edu.upeu.envio.entity.Envio;
import pe.edu.upeu.envio.entity.EstadoEnvio;
import pe.edu.upeu.envio.event.DespachoSolicitadoEvento;
import pe.edu.upeu.envio.messaging.EnvioEventosProducer;
import pe.edu.upeu.envio.repository.EnvioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private EnvioEventosProducer envioEventosProducer;

    @InjectMocks
    private EnvioServiceImpl envioService;

    @Test
    @DisplayName("Caso 1 (Éxito): Procesa despacho nuevo con dirección válida -> PROGRAMADO y publica envio.programado")
    void testProcesarDespachoExitoso() {
        // Arrange
        DespachoSolicitadoEvento evento = DespachoSolicitadoEvento.builder()
                .tipoEvento("despacho.solicitado")
                .ordenId(10L)
                .idCliente(1L)
                .direccionEnvio("Av. Primavera 123, Lima")
                .total(new BigDecimal("150.00"))
                .origen("pagatu-orden-ms")
                .timestamp(System.currentTimeMillis())
                .build();

        when(envioRepository.findByOrdenId(10L)).thenReturn(Optional.empty());
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> {
            Envio e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        // Act
        envioService.procesarDespacho(evento);

        // Assert
        verify(envioRepository).save(argThat(e ->
                e.getOrdenId().equals(10L) &&
                e.getEstado() == EstadoEnvio.PROGRAMADO &&
                "Av. Primavera 123, Lima".equals(e.getDireccionEnvio()) &&
                e.getGuiaSeguimiento() != null
        ));
        verify(envioEventosProducer).publicarTrasCommit(argThat(evt ->
                evt.getOrdenId().equals(10L) &&
                "envio.programado".equals(evt.getTipoEvento()) &&
                "PROGRAMADO".equals(evt.getEstado())
        ));
    }

    @Test
    @DisplayName("Caso 1b (Idempotencia Nivel 1): Evento duplicado ya existe en BD -> se ignora y no publica duplicado")
    void testIdempotenciaEventoDuplicadoIgnorado() {
        // Arrange
        DespachoSolicitadoEvento evento = DespachoSolicitadoEvento.builder()
                .tipoEvento("despacho.solicitado")
                .ordenId(20L)
                .idCliente(2L)
                .direccionEnvio("Calle Los Pinos 456, Arequipa")
                .total(new BigDecimal("80.00"))
                .origen("pagatu-orden-ms")
                .timestamp(System.currentTimeMillis())
                .build();

        Envio envioExistente = Envio.builder()
                .id(2L)
                .ordenId(20L)
                .direccionEnvio("Calle Los Pinos 456, Arequipa")
                .estado(EstadoEnvio.PROGRAMADO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(envioRepository.findByOrdenId(20L)).thenReturn(Optional.of(envioExistente));

        // Act
        envioService.procesarDespacho(evento);

        // Assert
        verify(envioRepository, never()).save(any());
        verify(envioEventosProducer, never()).publicarTrasCommit(any());
    }

    @Test
    @DisplayName("Caso 1c (Idempotencia Nivel 2): Concurrencia capturada por DataIntegrityViolationException")
    void testIdempotenciaConcurrenciaDataIntegrityViolation() {
        // Arrange
        DespachoSolicitadoEvento evento = DespachoSolicitadoEvento.builder()
                .tipoEvento("despacho.solicitado")
                .ordenId(30L)
                .idCliente(3L)
                .direccionEnvio("Jr. Union 789, Cusco")
                .total(new BigDecimal("220.00"))
                .origen("pagatu-orden-ms")
                .timestamp(System.currentTimeMillis())
                .build();

        when(envioRepository.findByOrdenId(30L)).thenReturn(Optional.empty());
        when(envioRepository.save(any(Envio.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_envios_orden"));

        // Act & Assert (no debe propagar excepción, debe ser manejado limpiamente)
        assertDoesNotThrow(() -> envioService.procesarDespacho(evento));
        verify(envioEventosProducer, never()).publicarTrasCommit(any());
    }

    @Test
    @DisplayName("Caso 2 (Fallo y Compensación): Dirección contiene ZONA_NO_COBERTURA -> RECHAZADO y publica envio.fallido")
    void testFalloZonaNoCoberturaEmiteEnvioFallido() {
        // Arrange
        DespachoSolicitadoEvento evento = DespachoSolicitadoEvento.builder()
                .tipoEvento("despacho.solicitado")
                .ordenId(40L)
                .idCliente(4L)
                .direccionEnvio("ZONA_NO_COBERTURA Cerro Azul")
                .total(new BigDecimal("50.00"))
                .origen("pagatu-orden-ms")
                .timestamp(System.currentTimeMillis())
                .build();

        when(envioRepository.findByOrdenId(40L)).thenReturn(Optional.empty());
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocation -> {
            Envio e = invocation.getArgument(0);
            e.setId(4L);
            return e;
        });

        // Act
        envioService.procesarDespacho(evento);

        // Assert
        verify(envioRepository).save(argThat(e ->
                e.getOrdenId().equals(40L) &&
                e.getEstado() == EstadoEnvio.RECHAZADO &&
                e.getMotivoRechazo() != null &&
                e.getMotivoRechazo().contains("ZONA_NO_COBERTURA")
        ));
        verify(envioEventosProducer).publicarTrasCommit(argThat(evt ->
                evt.getOrdenId().equals(40L) &&
                "envio.fallido".equals(evt.getTipoEvento()) &&
                "RECHAZADO".equals(evt.getEstado()) &&
                evt.getMotivo() != null && evt.getMotivo().contains("ZONA_NO_COBERTURA")
        ));
    }
}
