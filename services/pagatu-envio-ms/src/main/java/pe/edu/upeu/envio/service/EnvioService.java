package pe.edu.upeu.envio.service;

import pe.edu.upeu.envio.dto.EnvioResponse;
import pe.edu.upeu.envio.event.DespachoSolicitadoEvento;
import java.util.List;

public interface EnvioService {
    void procesarDespacho(DespachoSolicitadoEvento evento);
    List<EnvioResponse> listar();
    EnvioResponse obtenerPorOrdenId(Long ordenId);
}

