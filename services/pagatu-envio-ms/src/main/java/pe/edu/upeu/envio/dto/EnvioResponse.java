package pe.edu.upeu.envio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.upeu.envio.entity.EstadoEnvio;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvioResponse {
    private Long id;
    private Long ordenId;
    private String direccionEnvio;
    private EstadoEnvio estado;
    private String guiaSeguimiento;
    private String motivoRechazo;
    private LocalDateTime fechaCreacion;
}

