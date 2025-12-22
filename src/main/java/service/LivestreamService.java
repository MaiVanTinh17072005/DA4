package service;

import dto.CreateLivestreamRequest;
import dto.LivestreamDTO;
import model.Livestream;
import model.User;
import repository.LivestreamRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing livestreams
 */
@Service
public class LivestreamService {
    
    @Autowired
    private LivestreamRepository livestreamRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new livestream
     */
    @Transactional
    public LivestreamDTO createLivestream(Long hostId, CreateLivestreamRequest request) {
        // Validate user exists
        Optional<User> userOpt = userRepository.findById(hostId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User host = userOpt.get();
        
        // Create livestream
        Livestream livestream = new Livestream(hostId, request.getTitle(), request.getDescription());
        livestream = livestreamRepository.save(livestream);
        
        System.out.println("✅ [LivestreamService] Created livestream: " + livestream.getStreamId() + " by " + host.getUsername());
        
        return convertToDTO(livestream, host);
    }
    
    /**
     * Get all active livestreams
     */
    public List<LivestreamDTO> getActiveLivestreams() {
        List<Livestream> livestreams = livestreamRepository.findByStatus("active");
        
        return livestreams.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get livestream by ID
     */
    public Optional<LivestreamDTO> getLivestreamById(Long streamId) {
        Optional<Livestream> livestreamOpt = livestreamRepository.findById(streamId);
        
        if (livestreamOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Livestream livestream = livestreamOpt.get();
        return Optional.of(convertToDTO(livestream));
    }
    
    /**
     * End a livestream
     */
    @Transactional
    public void endLivestream(Long streamId, Long hostId) {
        Optional<Livestream> livestreamOpt = livestreamRepository.findById(streamId);
        
        if (livestreamOpt.isEmpty()) {
            throw new RuntimeException("Livestream not found");
        }
        
        Livestream livestream = livestreamOpt.get();
        
        // Verify host
        if (!livestream.getHostId().equals(hostId)) {
            throw new RuntimeException("Only the host can end the livestream");
        }
        
        livestream.setStatus("ended");
        livestream.setEndTime(LocalDateTime.now());
        livestreamRepository.save(livestream);
        
        System.out.println("🛑 [LivestreamService] Ended livestream: " + streamId);
    }
    
    /**
     * Increment viewer count
     */
    @Transactional
    public void incrementViewerCount(Long streamId) {
        Optional<Livestream> livestreamOpt = livestreamRepository.findById(streamId);
        
        if (livestreamOpt.isPresent()) {
            Livestream livestream = livestreamOpt.get();
            livestream.setViewCount(livestream.getViewCount() + 1);
            livestreamRepository.save(livestream);
        }
    }
    
    /**
     * Decrement viewer count
     */
    @Transactional
    public void decrementViewerCount(Long streamId) {
        Optional<Livestream> livestreamOpt = livestreamRepository.findById(streamId);
        
        if (livestreamOpt.isPresent()) {
            Livestream livestream = livestreamOpt.get();
            int newCount = Math.max(0, livestream.getViewCount() - 1);
            livestream.setViewCount(newCount);
            livestreamRepository.save(livestream);
        }
    }
    
    /**
     * Convert Livestream entity to DTO
     */
    private LivestreamDTO convertToDTO(Livestream livestream) {
        if (livestream == null) {
            return null;
        }
        
        try {
            Optional<User> hostOpt = userRepository.findById(livestream.getHostId());
            return convertToDTO(livestream, hostOpt.orElse(null));
        } catch (Exception e) {
            System.err.println("⚠️ [LivestreamService] Error fetching user for livestream: " + e.getMessage());
            // Return DTO without user info if user fetch fails
            return convertToDTO(livestream, null);
        }
    }
    
    /**
     * Convert Livestream entity to DTO with User
     */
    private LivestreamDTO convertToDTO(Livestream livestream, User host) {
        if (livestream == null) {
            return null;
        }
        
        String hostName = (host != null) ? host.getUsername() : "Unknown User";
        
        LivestreamDTO dto = new LivestreamDTO();
        dto.setStreamId(livestream.getStreamId());
        dto.setHostId(livestream.getHostId());
        dto.setHostName(hostName);
        dto.setTitle(livestream.getTitle());
        dto.setDescription(livestream.getDescription());
        dto.setStartTime(livestream.getStartTime());
        dto.setEndTime(livestream.getEndTime());
        dto.setViewCount(livestream.getViewCount() != null ? livestream.getViewCount() : 0);
        dto.setStatus(livestream.getStatus());
        
        return dto;
    }
}
