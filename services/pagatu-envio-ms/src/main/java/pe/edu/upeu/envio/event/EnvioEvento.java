package pe.edu.upeu.envio.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvioEvento {
    private String tipoEvento;
    private Long ordenId;
    private String estado;
    private String guiaSeguimiento;
    private String motivo;
    private String origen;
    private Long timestamp;
}

