package it.wasacz.tuyaconnector;

import it.wasacz.tuyaconnector.external.TuyaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TuyaConfig.class)
public class TuyaConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuyaConnectorApplication.class, args);
    }

}
