package controller;

import dto.CreateLivestreamRequest;
import dto.LivestreamDTO;
import service.LivestreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Livestream operations
 */
@RestController
@RequestMapping("/api/v1/livestreams")
@CrossOrigin(origins = "*")
public class LivestreamController {
    
    @Autowired
    private LivestreamService livestreamService;
    
    /**
     * Create a new livestream
     * POST /api/v1/livestreams
     */
    @PostMapping
    public ResponseEntity<?> createLivestream(@RequestBody Map<String, Object> request) {
        try {
            Long hostId = Long.valueOf(request.get("hostId").toString());
            String title = request.get("title").toString();
            String description = request.get("description") != null ? request.get("description").toString() : "";
            
            CreateLivestreamRequest livestreamRequest = new CreateLivestreamRequest(title, description);
            LivestreamDTO livestream = livestreamService.createLivestream(hostId, livestreamRequest);
            
            System.out.println("📡 [LivestreamController] Created livestream: " + livestream.getStreamId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Livestream created successfully",
                "livestream", livestream
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error creating livestream: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get all active livestreams
     * GET /api/v1/livestreams/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveLivestreams() {
        try {
            List<LivestreamDTO> livestreams = livestreamService.getActiveLivestreams();
            
            System.out.println("📺 [LivestreamController] Retrieved " + livestreams.size() + " active livestreams");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "livestreams", livestreams
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error getting livestreams: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get livestream by ID
     * GET /api/v1/livestreams/{streamId}
     */
    @GetMapping("/{streamId}")
    public ResponseEntity<?> getLivestreamById(@PathVariable("streamId") Long streamId) {
        try {
            Optional<LivestreamDTO> livestreamOpt = livestreamService.getLivestreamById(streamId);
            
            if (livestreamOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Livestream not found"));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "livestream", livestreamOpt.get()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error getting livestream: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Join a livestream (increment viewer count)
     * POST /api/v1/livestreams/{streamId}/join
     */
    @PostMapping("/{streamId}/join")
    public ResponseEntity<?> joinLivestream(@PathVariable("streamId") Long streamId) {
        try {
            livestreamService.incrementViewerCount(streamId);
            
            System.out.println("👥 [LivestreamController] User joined livestream: " + streamId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Joined livestream successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error joining livestream: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Leave a livestream (decrement viewer count)
     * POST /api/v1/livestreams/{streamId}/leave
     */
    @PostMapping("/{streamId}/leave")
    public ResponseEntity<?> leaveLivestream(@PathVariable("streamId") Long streamId) {
        try {
            livestreamService.decrementViewerCount(streamId);
            
            System.out.println("👋 [LivestreamController] User left livestream: " + streamId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Left livestream successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error leaving livestream: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * End a livestream
     * POST /api/v1/livestreams/{streamId}/end
     */
    @PostMapping("/{streamId}/end")
    public ResponseEntity<?> endLivestream(@PathVariable("streamId") Long streamId, @RequestBody Map<String, Object> request) {
        try {
            Long hostId = Long.valueOf(request.get("hostId").toString());
            
            livestreamService.endLivestream(streamId, hostId);
            
            System.out.println("🛑 [LivestreamController] Ended livestream: " + streamId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Livestream ended successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("❌ [LivestreamController] Error ending livestream: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
