package pe.edu.upeu.envio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.envio.entity.Envio;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByOrdenId(Long ordenId);
}

