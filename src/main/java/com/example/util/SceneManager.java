package com.example.util;

import com.example.ui.MainLayoutController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Scene Manager Utility
 * Manages scene navigation and content swapping
 */
public class SceneManager {
    
    private static MainLayoutController mainLayoutController;
    private static Stage primaryStage;
    
    /**
     * Initialize SceneManager with main layout
     */
    public static void initialize(Stage stage) {
        primaryStage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/example/fxml/main-layout.fxml"));
            Parent root = loader.load();
            mainLayoutController = loader.getController();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneManager.class.getResource("/com/example/css/login.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize SceneManager: " + e.getMessage());
        }
    }
    
    /**
     * Load and display a content view
     * @param contentPath Path to content FXML (relative to /com/example/fxml/)
     * @return The loaded controller instance
     */
    public static Object loadContent(String contentPath) {
        if (mainLayoutController == null) {
            System.err.println("SceneManager not initialized!");
            return null;
        }
        return mainLayoutController.loadContent("/com/example/fxml/" + contentPath);
    }
    
    /**
     * Get the main layout controller
     */
    public static MainLayoutController getMainLayoutController() {
        return mainLayoutController;
    }
    
    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Set window title
     */
    public static void setTitle(String title) {
        if (primaryStage != null) {
            primaryStage.setTitle(title);
        }
    }
}

