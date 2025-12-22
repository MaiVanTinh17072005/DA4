package com.example.service;


import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * Camera Service for capturing webcam frames
 * Uses webcam-capture library (lightweight ~2MB)
 */
public class CameraService {
    
    private Webcam webcam;
    private boolean isCapturing = false;
    
    /**
     * Start camera capture
     * @param width Video width
     * @param height Video height
     */
    public void startCamera(int width, int height) throws Exception {
        System.out.println("📹 [CameraService] Starting camera...");
        
        // Get default webcam
        webcam = Webcam.getDefault();
        
        if (webcam == null) {
            throw new Exception("No webcam found!");
        }
        
        // Set resolution
        webcam.setViewSize(new Dimension(width, height));
        
        // Open webcam
        webcam.open();
        isCapturing = true;
        
        System.out.println("✅ [CameraService] Camera started: " + webcam.getName());
    }
    
    /**
     * Capture a single frame from camera
     * @return JavaFX Image
     */
    public Image captureFrame() {
        if (!isCapturing || webcam == null || !webcam.isOpen()) {
            return null;
        }
        
        try {
            // Get image from webcam
            BufferedImage bufferedImage = webcam.getImage();
            
            if (bufferedImage == null) {
                return null;
            }
            
            // Convert to JavaFX Image
            return SwingFXUtils.toFXImage(bufferedImage, null);
            
        } catch (Exception e) {
            System.err.println("❌ [CameraService] Error capturing frame: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Stop camera capture
     */
    public void stopCamera() {
        System.out.println("🛑 [CameraService] Stopping camera...");
        
        isCapturing = false;
        
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            System.out.println("✅ [CameraService] Camera stopped");
        }
    }
    
    /**
     * Check if camera is capturing
     */
    public boolean isCapturing() {
        return isCapturing && webcam != null && webcam.isOpen();
    }
    
    /**
     * Get camera name
     */
    public String getCameraName() {
        return webcam != null ? webcam.getName() : "Unknown";
    }
}
