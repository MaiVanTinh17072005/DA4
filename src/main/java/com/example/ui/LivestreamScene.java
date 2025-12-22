package com.example.ui;


import com.example.api.dto.UserDTO;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Livestream Scene
 * Manages livestream list, preview, and chat functionality
 */
public class LivestreamScene {

    // ===== SIDEBAR COMPONENTS =====
    @FXML private TextField searchField;
    @FXML private ListView<LivestreamItem> livestreamListView;
    @FXML private Circle userAvatarCircle;
    @FXML private Label currentUserNameLabel;
    @FXML private Label currentUserStatusLabel;

    // ===== MAIN AREA COMPONENTS =====
    @FXML private Label streamTitleLabel;
    @FXML private Label streamSubtitleLabel;
    @FXML private Button createStreamButton;
    @FXML private VBox streamPreviewContainer;
    @FXML private VBox videoPreviewSection;
    @FXML private VBox emptyStateSection;
    @FXML private Circle streamerAvatar;  // Avatar in stream info card
    @FXML private Label streamerNameLabel;
    @FXML private Label viewerCountLabel;
    @FXML private Label streamDescriptionLabel;
    @FXML private Label liveIndicator;

    // ===== CHAT PANEL COMPONENTS =====
    @FXML private VBox livestreamChatPanel;
    @FXML private ListView<String> chatMessagesListView;
    @FXML private TextArea chatInput;
    @FXML private ListView<String> viewerListView;

    private LivestreamItem selectedStream;

    @FXML
    public void initialize() {
        System.out.println("🎥 [LivestreamScene] Initializing...");

        // Load user profile
        loadUserProfile();

        // Setup livestream list
        setupLivestreamList();

        // Load active livestreams from API
        loadActiveLivestreams();
        
        // Start auto-refresh timer
        startAutoRefresh();

        // Setup search functionality
        setupSearch();

        System.out.println("✅ [LivestreamScene] Initialization complete");
    }



    /**
     * Load logged-in user profile
     */
    private void loadUserProfile() {
        UserDTO currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            currentUserNameLabel.setText(currentUser.getUsername());
            currentUserStatusLabel.setText("🟢 Online");

            // Load user avatar
            loadUserAvatar(currentUser.getAvatarUrl());
        }
    }

    /**
     * Load user avatar into the profile circle
     */
    private void loadUserAvatar(String avatarUrl) {
        try {
            javafx.scene.image.Image avatarImage;
            
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                // Try to load user's avatar
                try {
                    avatarImage = new javafx.scene.image.Image(getClass().getResourceAsStream(avatarUrl));
                    if (avatarImage.isError()) {
                        throw new Exception("Failed to load avatar from: " + avatarUrl);
                    }
                    System.out.println("[LivestreamScene] ✓ Loaded user avatar: " + avatarUrl);
                } catch (Exception e) {
                    System.out.println("[LivestreamScene] ⚠ Failed to load avatar, using default: " + e.getMessage());
                    avatarImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                }
            } else {
                // Load default avatar
                avatarImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                System.out.println("[LivestreamScene] Using default avatar");
            }
            
            if (userAvatarCircle != null && avatarImage != null && !avatarImage.isError()) {
                userAvatarCircle.setFill(new javafx.scene.paint.ImagePattern(avatarImage));
            }
        } catch (Exception e) {
            System.out.println("[LivestreamScene] ❌ Error loading avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set default avatar for a circle (used for streamer avatars)
     */
    private void setDefaultAvatar(Circle circle) {
        if (circle != null) {
            try {
                // Try to load default avatar image
                Image defaultImage = new Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                if (!defaultImage.isError()) {
                    circle.setFill(new ImagePattern(defaultImage));
                } else {
                    // Fallback to a gradient color if image fails
                    circle.setFill(Color.web("#5865F2")); // Discord blurple color
                }
            } catch (Exception e) {
                // Fallback to a solid color if image loading fails
                circle.setFill(Color.web("#5865F2")); // Discord blurple color
                System.out.println("[LivestreamScene] Using default color for avatar: " + e.getMessage());
            }
        }
    }

    /**
     * Setup livestream list with custom cell factory
     */
    private void setupLivestreamList() {
        livestreamListView.setCellFactory(param -> new LivestreamListCell());
        
        livestreamListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedStream = newVal;
                showStreamPreview(newVal);
            }
        });
    }

    /**
     * Load active livestreams from backend API
     */
    private void loadActiveLivestreams() {
        // Load in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                com.example.service.LivestreamService livestreamService = 
                    com.example.service.LivestreamService.getInstance();
                
                List<com.example.api.dto.LivestreamDTO> activeLivestreams = 
                    livestreamService.getActiveLivestreams();
                
                // Convert DTOs to UI items
                List<LivestreamItem> streamItems = new ArrayList<>();
                for (com.example.api.dto.LivestreamDTO dto : activeLivestreams) {
                    streamItems.add(new LivestreamItem(
                        dto.getStreamId(),
                        dto.getTitle(),
                        dto.getHostName(),
                        dto.getViewCount(),
                        dto.getDescription() != null ? dto.getDescription() : "",
                        "active".equals(dto.getStatus())
                    ));
                }
                
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    livestreamListView.getItems().clear();
                    livestreamListView.getItems().addAll(streamItems);
                    System.out.println("📺 [LivestreamScene] Loaded " + streamItems.size() + " active livestreams");
                    
                    // If a stream is currently selected, update its preview with fresh data
                    if (selectedStream != null) {
                        // Find the updated stream in the new list
                        for (LivestreamItem item : streamItems) {
                            if (item.getStreamId().equals(selectedStream.getStreamId())) {
                                selectedStream = item;  // Update reference
                                showStreamPreview(item);  // Refresh preview with new data
                                System.out.println("🔄 [LivestreamScene] Updated preview for: " + item.getTitle() + " (viewers: " + item.getViewerCount() + ")");
                                break;
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                // Graceful error handling - don't freeze UI
                System.err.println("⚠️ [LivestreamScene] Error loading livestreams: " + e.getMessage());
                
                javafx.application.Platform.runLater(() -> {
                    // Show empty state if API fails
                    if (livestreamListView.getItems().isEmpty()) {
                        System.out.println("📺 [LivestreamScene] No livestreams available (server may be offline)");
                    }
                });
            }
        }).start();
    }
    
    /**
     * Start auto-refresh timer for livestreams
     * Refreshes every 1 second for near real-time updates
     */
    private void startAutoRefresh() {
        javafx.animation.Timeline refreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),  // Update every 1 second
                event -> loadActiveLivestreams()
            )
        );
        refreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimeline.play();
        
        System.out.println("🔄 [LivestreamScene] Auto-refresh started (every 1 second)");
    }

    /**
     * Setup search functionality
     */
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // TODO: Implement search filtering
            System.out.println("🔍 [LivestreamScene] Search: " + newVal);
        });
    }

    /**
     * Show stream preview when a stream is selected
     */
    private void showStreamPreview(LivestreamItem stream) {
        // Hide empty state, show preview
        emptyStateSection.setVisible(false);
        emptyStateSection.setManaged(false);
        videoPreviewSection.setVisible(true);
        videoPreviewSection.setManaged(true);

        // Update stream info
        streamTitleLabel.setText(stream.getTitle());
        streamSubtitleLabel.setText("Đang phát bởi " + stream.getStreamerName());
        streamerNameLabel.setText(stream.getStreamerName());
        viewerCountLabel.setText("👥 " + stream.getViewerCount() + " viewers");
        streamDescriptionLabel.setText(stream.getDescription());

        // Set streamer avatar
        setDefaultAvatar(streamerAvatar);

        System.out.println("📺 [LivestreamScene] Showing preview for: " + stream.getTitle());
    }

    // ===== EVENT HANDLERS =====



    @FXML
    private void handleCreateStream() {
        System.out.println("📡 [LivestreamScene] Create Livestream clicked");
        
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tạo Livestream");
        dialog.setHeaderText("Bắt đầu phiên livestream của bạn");
        
        // Set dialog icon
        dialog.setGraphic(new Label("📡"));
        
        // Create form
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2b2d3a;");
        
        // Title field
        Label titleLabel = new Label("Tiêu đề livestream:");
        titleLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        TextField titleField = new TextField();
        titleField.setPromptText("Nhập tiêu đề livestream...");
        titleField.setStyle(
            "-fx-background-color: #1e2132; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-prompt-text-fill: #6b6e7a; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10px; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: rgba(88, 101, 242, 0.3); " +
            "-fx-border-radius: 6px;"
        );
        titleField.setPrefWidth(400);
        
        // Description field
        Label descLabel = new Label("Mô tả (tùy chọn):");
        descLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Nhập mô tả livestream...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(3);
        descArea.setStyle(
            "-fx-control-inner-background: #1e2132; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-prompt-text-fill: #6b6e7a; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: rgba(88, 101, 242, 0.3); " +
            "-fx-border-radius: 6px;"
        );
        
        content.getChildren().addAll(titleLabel, titleField, descLabel, descArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #2b2d3a;");
        
        // Add buttons
        ButtonType createButtonType = new ButtonType("Bắt đầu phát", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, cancelButtonType);
        
        // Style buttons
        Platform.runLater(() -> {
            Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
            createButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #5dd679 0%, #4bc861 100%); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10px 20px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand;"
            );
            
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
            cancelButton.setStyle(
                "-fx-background-color: #3a3d4e; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 10px 20px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand;"
            );
        });
        
        // Handle result
        dialog.showAndWait().ifPresent(result -> {
            if (result == createButtonType) {
                String title = titleField.getText().trim();
                String description = descArea.getText().trim();
                
                if (title.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập tiêu đề livestream");
                    return;
                }
                
                // Create livestream in background thread
                new Thread(() -> {
                    try {
                        com.example.service.LivestreamService livestreamService = 
                            com.example.service.LivestreamService.getInstance();
                        
                        com.example.api.dto.LivestreamDTO livestream = 
                            livestreamService.createLivestream(title, description);
                        
                        Platform.runLater(() -> {
                            System.out.println("✅ [LivestreamScene] Livestream created: " + livestream.getStreamId());
                            
                            // Open broadcast window
                            LivestreamBroadcastWindow broadcastWindow = new LivestreamBroadcastWindow(livestream);
                            broadcastWindow.show();
                            
                            // Reload livestream list from API
                            loadActiveLivestreams();
                        });
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            System.err.println("❌ [LivestreamScene] Error creating livestream: " + e.getMessage());
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không thể tạo livestream: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleJoinStream() {
        if (selectedStream != null) {
            System.out.println("▶ [LivestreamScene] Joining stream: " + selectedStream.getTitle());
            
            // Fetch full livestream details from API
            new Thread(() -> {
                try {
                    com.example.service.LivestreamService livestreamService = 
                        com.example.service.LivestreamService.getInstance();
                    
                    com.example.api.dto.LivestreamDTO livestream = 
                        livestreamService.getLivestreamById(selectedStream.getStreamId());
                    
                    // Check if current user is the host
                    Long currentUserId = SessionManager.getCurrentUser().getId();
                    if (livestream.getHostId().equals(currentUserId)) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Không thể tham gia");
                            alert.setHeaderText("Bạn là host của livestream này");
                            alert.setContentText("Bạn không thể tham gia livestream của chính mình. Bạn đang phát livestream này.");
                            alert.showAndWait();
                        });
                        return;
                    }
                    
                    Platform.runLater(() -> {
                        // Open viewer window
                        LivestreamViewerWindow viewerWindow = new LivestreamViewerWindow(livestream);
                        viewerWindow.show();
                        
                        System.out.println("✅ [LivestreamScene] Opened viewer window for stream: " + livestream.getStreamId());
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        System.err.println("❌ [LivestreamScene] Error joining livestream: " + e.getMessage());
                        e.printStackTrace();
                        
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Lỗi");
                        alert.setHeaderText("Không thể tham gia livestream");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleShareStream() {
        if (selectedStream != null) {
            System.out.println("🔗 [LivestreamScene] Sharing stream: " + selectedStream.getTitle());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chia sẻ Livestream");
            alert.setHeaderText("Link đã được sao chép!");
            alert.setContentText("Bạn có thể chia sẻ link livestream với bạn bè của mình.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCloseChat() {
        System.out.println("❌ [LivestreamScene] Close chat panel");
        // Hide chat panel
        if (livestreamChatPanel != null) {
            livestreamChatPanel.setVisible(false);
            livestreamChatPanel.setManaged(false);
        }
    }

    @FXML
    private void handleSendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            System.out.println("💬 [LivestreamScene] Sending chat message: " + message);
            // TODO: Send chat message to livestream
            chatInput.clear();
        }
    }

    @FXML
    private void handleOpenProfile() {
        System.out.println("👤 [LivestreamScene] Opening profile");
        SceneManager.setTitle("Discord Mini - Hồ sơ");
        SceneManager.loadContent("content/profile-content.fxml");
    }



    // ===== INNER CLASSES =====

    /**
     * Data model for livestream item
     */
    public static class LivestreamItem {
        private final Long streamId;
        private final String title;
        private final String streamerName;
        private final int viewerCount;
        private final String description;
        private final boolean isLive;

        public LivestreamItem(Long streamId, String title, String streamerName, int viewerCount, String description, boolean isLive) {
            this.streamId = streamId;
            this.title = title;
            this.streamerName = streamerName;
            this.viewerCount = viewerCount;
            this.description = description;
            this.isLive = isLive;
        }

        public Long getStreamId() { return streamId; }
        public String getTitle() { return title; }
        public String getStreamerName() { return streamerName; }
        public int getViewerCount() { return viewerCount; }
        public String getDescription() { return description; }
        public boolean isLive() { return isLive; }
    }

    /**
     * Custom ListCell for livestream items
     */
    private static class LivestreamListCell extends ListCell<LivestreamItem> {
        private final HBox container;
        private final VBox thumbnailPlaceholder;
        private final VBox infoBox;
        private final Label titleLabel;
        private final Label streamerLabel;
        private final Label viewersLabel;
        private final Label liveBadge;

        public LivestreamListCell() {
            container = new HBox();
            container.setSpacing(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("livestream-cell");
            container.setPadding(new Insets(8));

            // Thumbnail placeholder
            thumbnailPlaceholder = new VBox();
            thumbnailPlaceholder.setPrefSize(80, 45);
            thumbnailPlaceholder.setMinSize(80, 45);
            thumbnailPlaceholder.setMaxSize(80, 45);
            thumbnailPlaceholder.getStyleClass().add("livestream-thumbnail");
            thumbnailPlaceholder.setAlignment(Pos.CENTER);
            
            // Live badge
            liveBadge = new Label("🔴 LIVE");
            liveBadge.getStyleClass().add("live-badge");
            liveBadge.setFont(Font.font(9));
            thumbnailPlaceholder.getChildren().add(liveBadge);

            // Info box
            infoBox = new VBox();
            infoBox.setSpacing(4);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            titleLabel = new Label();
            titleLabel.getStyleClass().add("livestream-title");
            titleLabel.setWrapText(false);
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            streamerLabel = new Label();
            streamerLabel.getStyleClass().add("livestream-streamer");

            viewersLabel = new Label();
            viewersLabel.getStyleClass().add("livestream-viewers");

            infoBox.getChildren().addAll(titleLabel, streamerLabel, viewersLabel);
            container.getChildren().addAll(thumbnailPlaceholder, infoBox);
        }

        @Override
        protected void updateItem(LivestreamItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(item.getTitle());
                streamerLabel.setText(item.getStreamerName());
                viewersLabel.setText("👥 " + item.getViewerCount() + " viewers");
                liveBadge.setVisible(item.isLive());
                
                setGraphic(container);
            }
        }
    }
}
