package pe.edu.upeu.envio.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.envio.dto.EnvioResponse;
import pe.edu.upeu.envio.service.EnvioService;

import java.util.List;

@Tag(name = "Envios", description = "Endpoints para gestion y seguimiento de envios")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/envios")
@RequiredArgsConstructor
public class EnvioController {

    private final EnvioService envioService;

    @GetMapping
    public List<EnvioResponse> listar() {
        return envioService.listar();
    }

    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<EnvioResponse> obtenerPorOrdenId(@PathVariable Long ordenId) {
        EnvioResponse envio = envioService.obtenerPorOrdenId(ordenId);
        if (envio == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(envio);
    }
}

