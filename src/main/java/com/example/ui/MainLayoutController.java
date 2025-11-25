package com.example.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Main Layout Controller
 * Manages the shared background and footer, swaps only the content card
 */
public class MainLayoutController {

    @FXML
    private StackPane contentContainer;

    /**
     * Load and display a content view
     * @param contentPath Path to the content FXML file
     * @return The loaded controller instance
     */
    public Object loadContent(String contentPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(contentPath));
            Parent content = loader.load();
            
            // Clear previous content
            contentContainer.getChildren().clear();
            
            // Adjust alignment based on content type
            // Chat interface needs full screen (TOP_LEFT), login forms need centering
            if (contentPath.contains("chat.fxml")) {
                contentContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            } else {
                contentContainer.setAlignment(javafx.geometry.Pos.CENTER);
            }
            
            // Add new content
            contentContainer.getChildren().add(content);
            
            // Return controller for further configuration
            return loader.getController();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load content: " + contentPath);
            return null;
        }
    }

    /**
     * Get the content container for direct manipulation
     */
    public StackPane getContentContainer() {
        return contentContainer;
    }

    /**
     * Clear current content
     */
    public void clearContent() {
        contentContainer.getChildren().clear();
    }
}

