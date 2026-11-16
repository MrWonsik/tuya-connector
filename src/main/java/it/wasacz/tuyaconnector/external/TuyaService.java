package it.wasacz.tuyaconnector.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
@Service
public class TuyaService {

    private static final Logger log = LoggerFactory.getLogger(TuyaService.class);

    private static final String TOKEN_PATH = "/v1.0/token?grant_type=1";
    private static final String HTTP_GET = "GET";
    private static final String EMPTY_BODY = "";
    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGN_METHOD = "HMAC-SHA256";

    private final TuyaConfig tuyaConfig;
    private final RestTemplate restTemplate;

    public TuyaService(TuyaConfig tuyaConfig) {
        this.tuyaConfig = tuyaConfig;
        this.restTemplate = new RestTemplate();
    }

    public Optional<String> getAccessToken() {
        long timestamp = System.currentTimeMillis();

        try {
            ResponseEntity<TuyaTokenResponse> response = fetchAccessToken(timestamp);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Tuya token request failed with status: {}", response.getStatusCode());
                return Optional.empty();
            }

            return extractAccessToken(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Tuya token request failed with status: {}", e.getStatusCode());
            return Optional.empty();
        }
    }

    private ResponseEntity<TuyaTokenResponse> fetchAccessToken(long timestamp) {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(timestamp));

        try {
            return restTemplate.exchange(
                    tuyaConfig.endpoint() + TOKEN_PATH,
                    HttpMethod.GET,
                    entity,
                    TuyaTokenResponse.class
            );
        } catch (HttpStatusCodeException e) {
            throw new TuyaAuthorizationException(
                    "Tuya token request failed with status: " + e.getStatusCode(),
                    e
            );
        }
    }

    private HttpHeaders createAuthHeaders(long timestamp) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("client_id", tuyaConfig.clientId());
        headers.set("sign", calculateTuyaSign(timestamp));
        headers.set("t", String.valueOf(timestamp));
        headers.set("sign_method", SIGN_METHOD);

        return headers;
    }

    private Optional<String> extractAccessToken(TuyaTokenResponse response) {
        if (!Boolean.TRUE.equals(response.success())) {
            log.warn("Tuya authorization failed. Code: {}, message: {}", response.code(), response.msg());
            return Optional.empty();
        }

        if (response.result() == null || response.result().accessToken() == null) {
            log.warn("Tuya token response does not contain access token");
            return Optional.empty();
        }

        return Optional.of(response.result().accessToken());
    }

    private String calculateTuyaSign(long timestamp) {
        String stringToSign = HTTP_GET + "\n" + sha256(EMPTY_BODY) + "\n" + "\n" + TOKEN_PATH;
        String contentToSign = tuyaConfig.clientId() + timestamp + stringToSign;

        try {

            Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    tuyaConfig.secret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(contentToSign.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash).toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("Could not calculate Tuya request signature", e);
        }
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance(SHA_256)
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new TuyaAuthorizationException("Could not calculate SHA-256 hash", e);
        }
    }

    private record TuyaTokenResponse(
            Boolean success,
            String code,
            String msg,
            TuyaTokenResult result
    ) {
    }

    private record TuyaTokenResult(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }


}