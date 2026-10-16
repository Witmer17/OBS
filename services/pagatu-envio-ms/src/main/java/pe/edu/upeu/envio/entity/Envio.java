package pe.edu.upeu.envio.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "envios", uniqueConstraints = {
    @UniqueConstraint(name = "uk_envios_orden", columnNames = "orden_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "direccion_envio", nullable = false)
    private String direccionEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoEnvio estado;

    @Column(name = "guia_seguimiento", length = 100)
    private String guiaSeguimiento;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}

