package pe.edu.upeu.envio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PagatuEnvioMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagatuEnvioMsApplication.class, args);
    }
}

