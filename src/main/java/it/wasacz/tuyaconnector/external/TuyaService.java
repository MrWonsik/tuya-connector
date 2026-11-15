package it.wasacz.tuyaconnector.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@Service
public class TuyaService {

    private static final Logger log = LoggerFactory.getLogger(TuyaService.class);

    private static final String TUYA_AUTH_TOKEN_PATH = "/v1.0/token?grant_type=1";
    private static final String EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private final TuyaConfig tuyaConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public TuyaService(TuyaConfig tuyaConfig) {
        this.tuyaConfig = tuyaConfig;
    }

    public String getAccessToken() {
        long t = System.currentTimeMillis();

        String sign = calculateSign(
                tuyaConfig.clientId(),
                tuyaConfig.secret(),
                t,
                "GET",
                TUYA_AUTH_TOKEN_PATH
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("client_id", tuyaConfig.clientId());
        headers.set("sign", sign);
        headers.set("t", String.valueOf(t));
        headers.set("sign_method", "HMAC-SHA256");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tuyaConfig.endpoint() + TUYA_AUTH_TOKEN_PATH,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            log.info("Odpowiedź Tuya token API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                Boolean success = (Boolean) body.get("success");
                if (!Boolean.TRUE.equals(success)) {
                    log.error("Tuya zwróciła błąd autoryzacji. Code: {}, Msg: {}, Full body: {}",
                            body.get("code"),
                            body.get("msg"),
                            body
                    );
                    return null;
                }

                Map<String, Object> result = (Map<String, Object>) body.get("result");
                if (result != null) {
                    return (String) result.get("access_token");
                }

                log.error("Brak pola result w odpowiedzi Tuya: {}", body);
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("HTTP error podczas pobierania tokena Tuya. Status: {}, Body: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            log.error("Błąd podczas pobierania tokena Tuya", e);
        }

        return null;
    }

    private String calculateSign(String clientId, String secret, long t, String method, String url) {
        try {
            String stringToSign = method + "\n" + EMPTY_BODY_SHA256 + "\n" + "\n" + url;
            String contentString = clientId + t + stringToSign;

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(contentString.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Nie udało się wygenerować podpisu Tuya", e);
        }
    }
}