package it.wasacz.tuyaconnector;

import it.wasacz.tuyaconnector.external.TuyaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tuya")
public class TuyaConnectorController {

    private final TuyaService tuyaService;

    public TuyaConnectorController(TuyaService tuyaService) {
        this.tuyaService = tuyaService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        String token = tuyaService.getAccessToken();
        if (token != null) {
            return ResponseEntity.ok("Sukces! Pobrano token Tuya: " + token);
        } else {
            return ResponseEntity.internalServerError().body("Błąd autoryzacji z Tuya. Sprawdź logi.");
        }
    }
}
