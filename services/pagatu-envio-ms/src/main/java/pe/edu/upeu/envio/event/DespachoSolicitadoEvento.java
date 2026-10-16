package pe.edu.upeu.envio.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoSolicitadoEvento {
    private String tipoEvento;
    private Long ordenId;
    private String idCliente;
    private String direccionEnvio;
    private BigDecimal total;
    private String origen;
    private Long timestamp;
}

