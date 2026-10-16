package pe.edu.upeu.orden.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenRequest {

    @NotBlank
    private String metodoPago;

    private String direccionCliente;

    @NotEmpty
    @Valid
    private List<DetalleOrdenRequest> detalles;
}
