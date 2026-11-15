package it.wasacz.tuyaconnector.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tuya")
public record TuyaConfig(
        String endpoint,
        String clientId,
        String secret
) {
}
