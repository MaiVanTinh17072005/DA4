package com.example.ui;

import com.example.api.dto.LivestreamDTO;
import com.example.service.LivestreamService;
import com.example.service.LivestreamWebSocketClient;
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
 * Livestream Viewer Window
 * Shows livestream preview for viewers (not broadcasters)
 */
public class LivestreamViewerWindow {
    
    private Stage stage;
    private LivestreamDTO livestream;
    private Label viewerCountLabel;
    private Label statusLabel;
    private boolean isViewing = false;
    private Thread viewerCountUpdateThread;
    private LivestreamWebSocketClient webSocketClient;
    private ImageView videoView; // For displaying live video
    
    public LivestreamViewerWindow(LivestreamDTO livestream) {
        this.livestream = livestream;
        this.isViewing = true;
        createWindow();
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("📺 " + livestream.getTitle());
        
        // Set minimum size
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        // Windowed mode with maximize
        stage.setMaximized(true);
        
        // Main container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000;");
        
        // Top bar - Stream info
        HBox topBar = createTopBar();
        root.setTop(topBar);
        
        // Center - Video preview placeholder
        StackPane videoPreview = createVideoPreview();
        root.setCenter(videoPreview);
        
        // Bottom - Controls
        HBox controls = createControls();
        root.setBottom(controls);
        
        // Create scene
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        
        // Handle window close
        stage.setOnCloseRequest(event -> {
            handleLeaveStream();
        });
        
        stage.show();
        
        // Join livestream and start viewer count updates
        joinLivestream();
    }
    
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.7), transparent);");
        topBar.setMinHeight(60);
        
        // Title
        Label titleLabel = new Label(livestream.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setMaxWidth(500);
        titleLabel.setStyle("-fx-text-overflow: ellipsis;");
        
        // Host name
        Label hostLabel = new Label("📡 " + livestream.getHostName());
        hostLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        hostLabel.setTextFill(Color.web("#b9bbbe"));
        
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
        
        topBar.getChildren().addAll(titleLabel, hostLabel, spacer, liveIndicator, viewerCountLabel);
        
        return topBar;
    }
    
    private StackPane createVideoPreview() {
        StackPane preview = new StackPane();
        preview.setStyle("-fx-background-color: #000000;");
        
        // Video ImageView for live stream (bottom layer)
        videoView = new ImageView();
        videoView.setPreserveRatio(false); // Fill the container
        videoView.setFitWidth(1280);
        videoView.setFitHeight(720);
        videoView.setSmooth(true);
        
        // Placeholder (shown until video starts) - top layer
        VBox placeholder = new VBox(20);
        placeholder.setId("placeholder"); // Add ID for easy reference
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-background-color: #1a1a1a;");
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setMaxHeight(Double.MAX_VALUE);
        
        // Camera icon (using emoji)
        Label cameraIcon = new Label("📹");
        cameraIcon.setFont(Font.font("System", 80));
        
        // Status message
        statusLabel = new Label("Đang kết nối đến livestream...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.WHITE);
        
        // Description
        if (livestream.getDescription() != null && !livestream.getDescription().isEmpty()) {
            Label descLabel = new Label(livestream.getDescription());
            descLabel.setFont(Font.font("System", 14));
            descLabel.setTextFill(Color.web("#b9bbbe"));
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(600);
            descLabel.setAlignment(Pos.CENTER);
            descLabel.setStyle("-fx-text-alignment: center;");
            
            placeholder.getChildren().addAll(cameraIcon, statusLabel, descLabel);
        } else {
            placeholder.getChildren().addAll(cameraIcon, statusLabel);
        }
        
        // Add in correct order: video first (back), placeholder second (front)
        preview.getChildren().addAll(videoView, placeholder);
        
        return preview;
    }
    
    private HBox createControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(20));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.8), transparent);");
        controls.setMinHeight(80);
        
        // Leave button
        Button leaveButton = new Button("🚪 Rời khỏi");
        leaveButton.setFont(Font.font("System", FontWeight.BOLD, 16));
        leaveButton.setStyle(
            "-fx-background-color: #ed4245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.4), 15, 0.4, 0, 4);"
        );
        
        leaveButton.setOnMouseEntered(e -> leaveButton.setStyle(
            "-fx-background-color: #f04245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.5), 18, 0.45, 0, 5);"
        ));
        
        leaveButton.setOnMouseExited(e -> leaveButton.setStyle(
            "-fx-background-color: #ed4245;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 15px 40px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(237, 66, 69, 0.4), 15, 0.4, 0, 4);"
        ));
        
        leaveButton.setOnAction(e -> handleLeaveStream());
        
        controls.getChildren().add(leaveButton);
        
        return controls;
    }
    
    /**
     * Join the livestream (increment viewer count)
     */
    private void joinLivestream() {
        new Thread(() -> {
            try {
                LivestreamService livestreamService = LivestreamService.getInstance();
                livestreamService.joinLivestream(livestream.getStreamId());
                
                System.out.println("✅ [LivestreamViewer] Joined livestream: " + livestream.getStreamId());
                
                // Connect to WebSocket for video streaming
                connectWebSocket();
                
                // Start viewer count updates
                startViewerCountUpdate();
                
            } catch (Exception e) {
                System.err.println("❌ [LivestreamViewer] Error joining livestream: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi");
                    alert.setHeaderText("Không thể tham gia livestream");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                    stage.close();
                });
            }
        }).start();
    }
    
    /**
     * Leave the livestream (decrement viewer count)
     */
    private void handleLeaveStream() {
        if (!isViewing) {
            stage.close();
            return;
        }
        
        isViewing = false;
        
        // Stop viewer count updates
        if (viewerCountUpdateThread != null) {
            viewerCountUpdateThread.interrupt();
        }
        
        // Disconnect WebSocket
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        
        // Leave livestream via API
        new Thread(() -> {
            try {
                LivestreamService livestreamService = LivestreamService.getInstance();
                livestreamService.leaveLivestream(livestream.getStreamId());
                
                System.out.println("✅ [LivestreamViewer] Left livestream: " + livestream.getStreamId());
                
                Platform.runLater(() -> {
                    stage.close();
                });
                
            } catch (Exception e) {
                System.err.println("❌ [LivestreamViewer] Error leaving livestream: " + e.getMessage());
                
                Platform.runLater(() -> {
                    stage.close();
                });
            }
        }).start();
    }
    
    /**
     * Start viewer count update timer
     * Updates every 1 second for near real-time updates
     */
    private void startViewerCountUpdate() {
        viewerCountUpdateThread = new Thread(() -> {
            while (isViewing) {
                try {
                    Thread.sleep(1000); // Update every 1 second
                    
                    if (!isViewing) break;
                    
                    // Fetch updated livestream info
                    LivestreamService livestreamService = LivestreamService.getInstance();
                    LivestreamDTO updated = livestreamService.getLivestreamById(livestream.getStreamId());
                    
                    Platform.runLater(() -> {
                        viewerCountLabel.setText("👁 " + updated.getViewCount());
                    });
                    
                    System.out.println("🔄 [LivestreamViewer] Updated viewer count: " + updated.getViewCount());
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ [LivestreamViewer] Error updating viewer count: " + e.getMessage());
                }
            }
        });
        viewerCountUpdateThread.setDaemon(true);
        viewerCountUpdateThread.start();
    }
    
    /**
     * Connect to WebSocket for receiving video stream
     */
    private void connectWebSocket() {
        webSocketClient = new LivestreamWebSocketClient();
        
        webSocketClient.connectAsViewer(
            livestream.getStreamId(),
            // On frame received
            (frame) -> {
                System.out.println("🎬 [Viewer] Frame callback triggered! Image: " + frame.getWidth() + "x" + frame.getHeight());
                
                // Update video view with received frame
                videoView.setImage(frame);
                
                // Hide placeholder on first frame
                StackPane parent = (StackPane) videoView.getParent();
                
                // Find and remove placeholder by ID
                parent.getChildren().removeIf(node -> "placeholder".equals(node.getId()));
                
                System.out.println("✅ [Viewer] Video displayed, placeholder removed");
            },
            // On stream ended
            (message) -> {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Livestream đã kết thúc");
                    alert.setHeaderText(null);
                    alert.setContentText("Livestream đã kết thúc bởi người phát.");
                    alert.showAndWait();
                    stage.close();
                });
            }
        ).thenRun(() -> {
            System.out.println("✅ [Viewer] Connected to WebSocket for viewing");
            Platform.runLater(() -> {
                statusLabel.setText("Đang xem livestream của " + livestream.getHostName());
            });
        }).exceptionally(error -> {
            System.err.println("❌ [Viewer] Failed to connect to WebSocket: " + error.getMessage());
            error.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ Lỗi kết nối video stream");
                statusLabel.setTextFill(Color.web("#ed4245"));
            });
            return null;
        });
    }
    
    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }
}

