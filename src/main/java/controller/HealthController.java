package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Health Controller
 * Provides health check endpoint for server monitoring
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {
    
    /**
     * Health check endpoint
     * GET /api/v1/health
     * 
     * @return Server status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Server is running");
    }
}
