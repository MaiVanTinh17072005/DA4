package com.example.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Application Entry Point
 * Loads Login Scene on startup
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load login FXML (theo cấu trúc com.example)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/login.fxml"));
            Parent root = loader.load();

            // Create scene with dark theme
            Scene scene = new Scene(root, 450, 650);
            scene.getStylesheets().add(getClass().getResource("/com/example/css/login.css").toExternalForm());

            // Configure stage
            primaryStage.setTitle("Discord Mini - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();

            // Show stage
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load login scene: " + e.getMessage());
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