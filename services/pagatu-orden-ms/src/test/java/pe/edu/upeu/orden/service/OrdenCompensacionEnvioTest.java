package pe.edu.upeu.orden.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.orden.entity.EstadoOrden;
import pe.edu.upeu.orden.entity.Orden;
import pe.edu.upeu.orden.entity.OrdenDetalle;
import pe.edu.upeu.orden.event.DespachoSolicitadoEvento;
import pe.edu.upeu.orden.messaging.OrdenEventosProducer;
import pe.edu.upeu.orden.repository.OrdenRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenCompensacionEnvioTest {

    @Mock
    private OrdenRepository ordenRepository;

    @Mock
    private ProductoConsultaService productoConsultaService;

    @Mock
    private OrdenEventosProducer producer;

    @InjectMocks
    private OrdenServiceImpl ordenService;

    @Test
    @DisplayName("Caso 2 (Compensación Saga): Orden PAGADA recibe fallo de despacho -> pasa a CANCELADA_ENVIO y restaura stock")
    void testCompensarEnvioExitoso() {
        // Arrange
        OrdenDetalle detalle = OrdenDetalle.builder()
                .id(1L)
                .idProducto(101L)
                .cantidad(2)
                .precioUnitario(new BigDecimal("50.00"))
                .build();

        Orden orden = Orden.builder()
                .id(1L)
                .idCliente(5L)
                .direccionCliente("ZONA_NO_COBERTURA Cerro Azul")
                .estado(EstadoOrden.PAGADA)
                .total(new BigDecimal("100.00"))
                .detalles(new ArrayList<>(List.of(detalle)))
                .build();
        detalle.setOrden(orden);

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        // Act
        ordenService.compensarEnvio(1L);

        // Assert
        assertEquals(EstadoOrden.CANCELADA_ENVIO, orden.getEstado());
        verify(productoConsultaService).restaurarStock(101L, 2);
    }

    @Test
    @DisplayName("Caso 3 (Idempotencia de Compensación): Segundo evento de fallo ignorado si orden ya está en CANCELADA_ENVIO")
    void testCompensarEnvioIdempotenteSegundoMensajeIgnorado() {
        // Arrange: la orden ya fue cancelada previamente por compensación de envío
        OrdenDetalle detalle = OrdenDetalle.builder()
                .id(2L)
                .idProducto(102L)
                .cantidad(4)
                .precioUnitario(new BigDecimal("25.00"))
                .build();

        Orden orden = Orden.builder()
                .id(2L)
                .idCliente(6L)
                .direccionCliente("ZONA_NO_COBERTURA Cerro Azul")
                .estado(EstadoOrden.CANCELADA_ENVIO) // Ya en estado final de compensación
                .total(new BigDecimal("100.00"))
                .detalles(new ArrayList<>(List.of(detalle)))
                .build();
        detalle.setOrden(orden);

        when(ordenRepository.findById(2L)).thenReturn(Optional.of(orden));

        // Act
        ordenService.compensarEnvio(2L);

        // Assert: no debe volver a restaurar stock (guarda de idempotencia)
        assertEquals(EstadoOrden.CANCELADA_ENVIO, orden.getEstado());
        verify(productoConsultaService, never()).restaurarStock(any(), any());
    }

    @Test
    @DisplayName("Caso Flujo Feliz: Envio exitoso -> orden pasa de PAGADA a DESPACHADA")
    void testMarcarDespachadaExitoso() {
        // Arrange
        Orden orden = Orden.builder()
                .id(3L)
                .idCliente(7L)
                .direccionCliente("Av. Brasil 123, Lima")
                .estado(EstadoOrden.PAGADA)
                .total(new BigDecimal("200.00"))
                .build();

        when(ordenRepository.findById(3L)).thenReturn(Optional.of(orden));

        // Act
        ordenService.marcarDespachada(3L);

        // Assert
        assertEquals(EstadoOrden.DESPACHADA, orden.getEstado());
    }

    @Test
    @DisplayName("Emisión de despacho al pagar: Orden en PENDIENTE_PAGO se marca PAGADA y emite despacho.solicitado")
    void testMarcarPagadaEmiteDespachoSolicitado() {
        // Arrange
        Orden orden = Orden.builder()
                .id(4L)
                .idCliente(8L)
                .direccionCliente("Av. Los Pinos 789")
                .estado(EstadoOrden.PENDIENTE_PAGO)
                .total(new BigDecimal("120.00"))
                .build();

        when(ordenRepository.findById(4L)).thenReturn(Optional.of(orden));

        // Act
        ordenService.marcarPagada(4L);

        // Assert
        assertEquals(EstadoOrden.PAGADA, orden.getEstado());
        verify(producer).publicarDespachoTrasCommit(argThat(evt ->
                evt.getOrdenId().equals(4L) &&
                "despacho.solicitado".equals(evt.getTipoEvento()) &&
                "Av. Los Pinos 789".equals(evt.getDireccionEnvio())
        ));
    }
}

