package com.example.app;

import com.example.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main Application Entry Point
 * Loads Main Layout with Login content on startup
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Initialize SceneManager with main layout
            SceneManager.initialize(primaryStage);
            
            // Load login content
            SceneManager.setTitle("Discord Mini - Login");
            SceneManager.loadContent("content/login-content.fxml");

            // Show stage
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        // Cleanup resources when application closes
        super.stop();
        System.out.println("Application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}