package com.example.ui;

import com.example.api.dto.LivestreamDTO;
import com.example.service.LivestreamService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Livestream Broadcasting Window
 * Shows camera preview and livestream controls
 */
public class LivestreamBroadcastWindow {
    
    private Stage stage;
    private LivestreamDTO livestream;
    private Label viewerCountLabel;
    private Label statusLabel;
    private boolean isLive = false;
    
    public LivestreamBroadcastWindow(LivestreamDTO livestream) {
        this.livestream = livestream;
        this.isLive = true;
        createWindow();
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("📡 Đang phát: " + livestream.getTitle());
        
        // Set minimum size to prevent UI breaking
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        // Windowed mode with maximize (not fullscreen)
        stage.setMaximized(true);
        
        // Main container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000;");
        
        // Top bar - Stream info (minimal)
        HBox topBar = createTopBarHorizontal();
        root.setTop(topBar);
        
        // Center - Camera preview
        StackPane cameraPreview = createCameraPreview();
        root.setCenter(cameraPreview);
        
        // Bottom - Controls
        HBox controls = createControls();
        root.setBottom(controls);
        
        // Create scene with default size
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        
        // Handle window close
        stage.setOnCloseRequest(event -> {
            if (isLive) {
                event.consume();
                handleStopStream();
            }
        });
        
        stage.show();
        
        // Start viewer count update (mock)
        startViewerCountUpdate();
    }
    
    private HBox createTopBarHorizontal() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.7), transparent);");
        topBar.setMinHeight(60);
        
        // Title (with max width to prevent overflow)
        Label titleLabel = new Label(livestream.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setMaxWidth(500);
        titleLabel.setStyle("-fx-text-overflow: ellipsis;");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Live indicator
        HBox liveIndicator = new HBox(8);
        liveIndicator.setAlignment(Pos.CENTER);
        liveIndicator.setStyle(
            "-fx-background-color: #ed4245; " +
            "-fx-background-radius: 4px; " +
            "-fx-padding: 6px 12px;"
        );
        
        Circle liveDot = new Circle(5);
        liveDot.setFill(Color.WHITE);
        
        Label liveLabel = new Label("LIVE");
        liveLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        liveLabel.setTextFill(Color.WHITE);
        
        liveIndicator.getChildren().addAll(liveDot, liveLabel);
        
        // Viewer count
        viewerCountLabel = new Label("👁 " + livestream.getViewCount());
        viewerCountLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        viewerCountLabel.setTextFill(Color.WHITE);
        
        topBar.getChildren().addAll(titleLabel, spacer, liveIndicator, viewerCountLabel);
        
        return topBar;
    }
    
    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #2b2d3a; -fx-border-color: #3a3d4e; -fx-border-width: 0 0 2 0;");
        
        // Title
        Label titleLabel = new Label(livestream.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#ffffff"));
        
        // Description
        Label descLabel = new Label(livestream.getDescription() != null ? livestream.getDescription() : "");
        descLabel.setFont(Font.font("System", 14));
        descLabel.setTextFill(Color.web("#b9bbbe"));
        
        // Status row
        HBox statusRow = new HBox(15);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        
        // Live indicator
        HBox liveIndicator = new HBox(8);
        liveIndicator.setAlignment(Pos.CENTER);
        liveIndicator.setStyle("-fx-background-color: #ed4245; -fx-background-radius: 4px; -fx-padding: 4px 10px;");
        
        Circle liveDot = new Circle(4);
        liveDot.setFill(Color.WHITE);
        
        Label liveLabel = new Label("LIVE");
        liveLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        liveLabel.setTextFill(Color.WHITE);
        
        liveIndicator.getChildren().addAll(liveDot, liveLabel);
        
        // Viewer count
        viewerCountLabel = new Label("👁 " + livestream.getViewCount() + " người xem");
        viewerCountLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        viewerCountLabel.setTextFill(Color.web("#ffffff"));
        
        // Stream ID
        Label streamIdLabel = new Label("ID: " + livestream.getStreamId());
        streamIdLabel.setFont(Font.font("System", 12));
        streamIdLabel.setTextFill(Color.web("#72767d"));
        
        statusRow.getChildren().addAll(liveIndicator, viewerCountLabel, streamIdLabel);
        
        topBar.getChildren().addAll(titleLabel, descLabel, statusRow);
        
        return topBar;
    }
    
    private StackPane createCameraPreview() {
        StackPane preview = new StackPane();
        preview.setStyle("-fx-background-color: #000000;");
        
        // Camera ImageView for real camera feed (cover mode - fill screen, maintain ratio)
        ImageView cameraView = new ImageView();
        cameraView.setPreserveRatio(true);
        
        // Smart binding to fill screen (cover mode like CSS)
        preview.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateCameraSize(cameraView, preview);
        });
        preview.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateCameraSize(cameraView, preview);
        });
        
        // Status overlay at bottom center
        VBox statusOverlay = new VBox(10);
        statusOverlay.setAlignment(Pos.CENTER);
        statusOverlay.setStyle(
            "-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.7), transparent);" +
            "-fx-padding: 20px;"
        );
        statusOverlay.setMaxHeight(100);
        StackPane.setAlignment(statusOverlay, Pos.BOTTOM_CENTER);
        
        statusLabel = new Label("🔴 Đang phát trực tiếp...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.web("#ed4245"));
        
        statusOverlay.getChildren().add(statusLabel);
        
        preview.getChildren().addAll(cameraView, statusOverlay);
        
        // Start camera capture
        startCameraCapture(cameraView);
        
        return preview;
    }
    
    /**
     * Update camera size to fill screen (cover mode)
     * Maintains aspect ratio, zooms to fill, crops excess
     */
    private void updateCameraSize(ImageView cameraView, StackPane container) {
        double containerWidth = container.getWidth();
        double containerHeight = container.getHeight();
        
        if (containerWidth <= 0 || containerHeight <= 0) return;
        
        javafx.scene.image.Image image = cameraView.getImage();
        if (image == null) {
            // Set default size while loading
            cameraView.setFitWidth(containerWidth);
            cameraView.setFitHeight(containerHeight);
            return;
        }
        
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        
        // Calculate scale to cover (fill) the entire container
        double scaleX = containerWidth / imageWidth;
        double scaleY = containerHeight / imageHeight;
        double scale = Math.max(scaleX, scaleY); // Use max to cover
        
        cameraView.setFitWidth(imageWidth * scale);
        cameraView.setFitHeight(imageHeight * scale);
    }
    
    private void startCameraCapture(ImageView cameraView) {
        // Show loading indicator
        Label loadingLabel = new Label("📹 Đang khởi động camera...");
        loadingLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        loadingLabel.setTextFill(Color.web("#ffffff"));
        ((StackPane)cameraView.getParent()).getChildren().add(loadingLabel);
        
        // Start camera in background thread to avoid UI lag
        new Thread(() -> {
            com.example.service.CameraService cameraService = new com.example.service.CameraService();
            
            try {
                // Start camera (640x480 resolution)
                cameraService.startCamera(640, 480);
                
                System.out.println("📹 [LivestreamBroadcast] Camera started: " + cameraService.getCameraName());
                
                Platform.runLater(() -> {
                    // Remove loading label
                    ((StackPane)cameraView.getParent()).getChildren().remove(loadingLabel);
                    
                    // Capture frames at 15 FPS (every 66ms) - reduced for better performance
                    javafx.animation.Timeline captureTimeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.millis(66), e -> {
                            try {
                                javafx.scene.image.Image frame = cameraService.captureFrame();
                                if (frame != null) {
                                    cameraView.setImage(frame);
                                }
                            } catch (Exception ex) {
                                System.err.println("❌ Error capturing frame: " + ex.getMessage());
                            }
                        })
                    );
                    captureTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
                    captureTimeline.play();
                    
                    // Stop camera when window closes
                    stage.setOnCloseRequest(event -> {
                        if (isLive) {
                            event.consume();
                            handleStopStream();
                        } else {
                            captureTimeline.stop();
                            cameraService.stopCamera();
                        }
                    });
                    
                    // Store references for cleanup
                    stage.getProperties().put("captureTimeline", captureTimeline);
                    stage.getProperties().put("cameraService", cameraService);
                });
                
            } catch (Exception e) {
                System.err.println("❌ [LivestreamBroadcast] Error starting camera: " + e.getMessage());
                e.printStackTrace();
                
                // Show error message
                Platform.runLater(() -> {
                    ((StackPane)cameraView.getParent()).getChildren().remove(loadingLabel);
                    
                    Label errorLabel = new Label("❌ Không thể truy cập camera\n" + e.getMessage());
                    errorLabel.setFont(Font.font("System", 14));
                    errorLabel.setTextFill(Color.web("#ed4245"));
                    errorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    cameraView.setImage(null);
                    ((StackPane)cameraView.getParent()).getChildren().add(errorLabel);
                });
            }
        }).start();
    }
    
    private HBox createControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(20));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.8), transparent);");
        controls.setMinHeight(80);
        
        // Stop button (larger, centered)
        Button stopButton = new Button("⏹ Dừng phát");
        stopButton.setFont(Font.font("System", FontWeight.BOLD, 16));
        stopButton.setStyle(
            "-fx-background-color: #ed4245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.4), 15, 0.4, 0, 4);"
        );
        
        stopButton.setOnMouseEntered(e -> stopButton.setStyle(
            "-fx-background-color: #f04245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.5), 18, 0.45, 0, 5);"
        ));
        
        stopButton.setOnMouseExited(e -> stopButton.setStyle(
            "-fx-background-color: #ed4245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.4), 15, 0.4, 0, 4);"
        ));
        
        stopButton.setOnAction(e -> handleStopStream());
        
        controls.getChildren().add(stopButton);
        
        return controls;
    }
    
    private void handleStopStream() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận");
        confirmAlert.setHeaderText("Dừng phát livestream?");
        confirmAlert.setContentText("Bạn có chắc chắn muốn kết thúc phiên livestream này không?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Stop camera first
                try {
                    javafx.animation.Timeline captureTimeline = 
                        (javafx.animation.Timeline) stage.getProperties().get("captureTimeline");
                    com.example.service.CameraService cameraService = 
                        (com.example.service.CameraService) stage.getProperties().get("cameraService");
                    
                    if (captureTimeline != null) {
                        captureTimeline.stop();
                    }
                    
                    if (cameraService != null) {
                        cameraService.stopCamera();
                    }
                } catch (Exception e) {
                    System.err.println("Error stopping camera: " + e.getMessage());
                }
                
                // End livestream via API
                new Thread(() -> {
                    try {
                        LivestreamService livestreamService = LivestreamService.getInstance();
                        livestreamService.endLivestream(livestream.getStreamId());
                        
                        Platform.runLater(() -> {
                            isLive = false;
                            statusLabel.setText("⏹ Đã dừng phát");
                            statusLabel.setTextFill(Color.web("#72767d"));
                            
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Thành công");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("Đã kết thúc phiên livestream thành công!");
                            successAlert.showAndWait();
                            
                            stage.close();
                        });
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Lỗi");
                            errorAlert.setHeaderText(null);
                            errorAlert.setContentText("Không thể kết thúc livestream: " + e.getMessage());
                            errorAlert.showAndWait();
                        });
                    }
                }).start();
            }
        });
    }
    
    private void startViewerCountUpdate() {
        // Mock viewer count update every 5 seconds
        Thread updateThread = new Thread(() -> {
            while (isLive) {
                try {
                    Thread.sleep(5000);
                    
                    if (!isLive) break;
                    
                    // Fetch updated livestream info
                    LivestreamService livestreamService = LivestreamService.getInstance();
                    LivestreamDTO updated = livestreamService.getLivestreamById(livestream.getStreamId());
                    
                    Platform.runLater(() -> {
                        viewerCountLabel.setText("👁 " + updated.getViewCount() + " người xem");
                    });
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Error updating viewer count: " + e.getMessage());
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }
}
