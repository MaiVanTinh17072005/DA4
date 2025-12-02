package com.example.ui;

import com.example.api.dto.UserDTO;
import com.example.service.FriendService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Add Friend Dialog
 * Handles user search and friend suggestions
 */
public class AddFriendDialog {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button closeButton;
    @FXML private VBox searchResultsContainer;
    @FXML private VBox searchResultsList;
    @FXML private VBox suggestionsContainer;

    private FriendService friendService;
    private Stage dialogStage;
    private List<Long> sentRequests = new ArrayList<>(); // Track sent friend requests
    
    // Static instance for refresh callback
    private static AddFriendDialog currentInstance;

    @FXML
    public void initialize() {
        friendService = new FriendService();
        currentInstance = this; // Track current instance
        loadSuggestions();
        setupSearchListener();
    }

    /**
     * Set the dialog stage for closing
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Setup real-time search listener
     */
    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                searchResultsContainer.setVisible(true);
                searchResultsContainer.setManaged(true);
            } else {
                searchResultsContainer.setVisible(false);
                searchResultsContainer.setManaged(false);
            }
        });
    }

    /**
     * Load random user suggestions
     */
    private void loadSuggestions() {
        List<UserDTO> suggestions = friendService.getRandomNonFriends(10);
        
        suggestionsContainer.getChildren().clear();
        
        if (suggestions.isEmpty()) {
            VBox emptyState = createEmptyState(
                "🔍",
                "Không có gợi ý nào",
                "Hiện tại không có người dùng nào để gợi ý"
            );
            suggestionsContainer.getChildren().add(emptyState);
        } else {
            for (UserDTO user : suggestions) {
                suggestionsContainer.getChildren().add(createUserCard(user));
            }
        }
    }
    
    /**
     * Static method to refresh suggestions from external callers
     * (e.g., when notification is received)
     */
    public static void refreshSuggestions() {
        if (currentInstance != null) {
            javafx.application.Platform.runLater(() -> {
                System.out.println("[AddFriendDialog] Refreshing suggestions...");
                currentInstance.loadSuggestions();
            });
        }
    }

    /**
     * Handle search button click
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập tên người dùng hoặc email để tìm kiếm", Alert.AlertType.WARNING);
            return;
        }

        performSearch(query.trim());
    }

    /**
     * Perform user search
     */
    private void performSearch(String query) {
        List<UserDTO> results = friendService.searchUsers(query);
        
        searchResultsList.getChildren().clear();
        
        if (results.isEmpty()) {
            VBox emptyState = createEmptyState(
                "😔",
                "Không tìm thấy kết quả",
                "Không tìm thấy người dùng nào với từ khóa \"" + query + "\""
            );
            searchResultsList.getChildren().add(emptyState);
        } else {
            for (UserDTO user : results) {
                searchResultsList.getChildren().add(createUserCard(user));
            }
        }
        
        searchResultsContainer.setVisible(true);
        searchResultsContainer.setManaged(true);
    }

    /**
     * Create a user card UI component
     */
    private HBox createUserCard(UserDTO user) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("user-card");

        // Avatar
        Circle avatar = new Circle(24);
        avatar.getStyleClass().add("user-avatar");
        
        // Load avatar image
        loadAvatar(avatar, user.getAvatarUrl());

        // User Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(user.getUsername());
        nameLabel.getStyleClass().add("user-name");

        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("user-email");

        Label statusLabel = new Label(getStatusText(user.getStatus()));
        statusLabel.getStyleClass().addAll("user-status", user.getStatus().toLowerCase());

        info.getChildren().addAll(nameLabel, emailLabel, statusLabel);

        // Add Friend Button
        Button addButton = new Button();
        
        // Check if request was already sent
        if (sentRequests.contains(user.getId())) {
            addButton.setText("⏳ Đã gửi");
            addButton.getStyleClass().add("pending-button");
        } else {
            addButton.setText("➕ Thêm bạn");
            addButton.getStyleClass().add("add-button");
        }
        
        addButton.setOnAction(e -> handleAddFriend(user, addButton));

        card.getChildren().addAll(avatar, info, addButton);
        return card;
    }

    /**
     * Handle add friend action (toggle between send and cancel)
     */
    private void handleAddFriend(UserDTO user, Button button) {
        // Check if request was already sent
        if (sentRequests.contains(user.getId())) {
            // Cancel friend request
            boolean success = friendService.cancelFriendRequest(user.getId());
            
            if (success) {
                // Remove from sent requests
                sentRequests.remove(user.getId());
                
                // Update button to show add state
                button.setText("➕ Thêm bạn");
                button.getStyleClass().clear();
                button.getStyleClass().add("add-button");
                
                showAlert(
                    "Đã thu hồi", 
                    "Đã thu hồi lời mời kết bạn với " + user.getUsername(),
                    Alert.AlertType.INFORMATION
                );
            } else {
                showAlert(
                    "Lỗi", 
                    "Không thể thu hồi lời mời kết bạn. Vui lòng thử lại sau.",
                    Alert.AlertType.ERROR
                );
            }
        } else {
            // Send friend request
            boolean success = friendService.sendFriendRequest(user.getId());
            
            if (success) {
                // Add to sent requests
                sentRequests.add(user.getId());
                
                // Update button to show pending state
                button.setText("⏳ Đã gửi");
                button.getStyleClass().clear();
                button.getStyleClass().add("pending-button");
                
                showAlert(
                    "Thành công", 
                    "Đã gửi lời mời kết bạn đến " + user.getUsername(),
                    Alert.AlertType.INFORMATION
                );
            } else {
                showAlert(
                    "Lỗi", 
                    "Không thể gửi lời mời kết bạn. Vui lòng thử lại sau.",
                    Alert.AlertType.ERROR
                );
            }
        }
    }

    /**
     * Create empty state UI
     */
    private VBox createEmptyState(String icon, String title, String message) {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("empty-state");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("empty-state-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("user-name");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-state-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        emptyState.getChildren().addAll(iconLabel, titleLabel, messageLabel);
        return emptyState;
    }
    
    /**
     * Load avatar image into Circle
     */
    private void loadAvatar(Circle avatarCircle, String avatarUrl) {
        System.out.println("[AddFriendDialog] Loading avatar: " + avatarUrl);
        try {
            javafx.scene.image.Image avatarImage;
            
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                // Try to load user's avatar
                try {
                    System.out.println("[AddFriendDialog] Attempting to load from resources: " + avatarUrl);
                    avatarImage = new javafx.scene.image.Image(
                        getClass().getResourceAsStream(avatarUrl));
                    if (avatarImage.isError()) {
                        throw new Exception("Failed to load avatar from: " + avatarUrl);
                    }
                    System.out.println("[AddFriendDialog] ✓ Successfully loaded avatar: " + avatarUrl);
                } catch (Exception e) {
                    System.out.println("[AddFriendDialog] ⚠ Failed to load avatar '" + avatarUrl + "': " + e.getMessage());
                    System.out.println("[AddFriendDialog] Using default avatar");
                    // Fallback to default avatar
                    avatarImage = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                }
            } else {
                System.out.println("[AddFriendDialog] Avatar URL is null or empty, using default");
                // Load default avatar
                avatarImage = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
            }
            
            if (avatarCircle != null && avatarImage != null && !avatarImage.isError()) {
                avatarCircle.setFill(new javafx.scene.paint.ImagePattern(avatarImage));
                System.out.println("[AddFriendDialog] ✓ Avatar set to circle");
            } else {
                System.err.println("[AddFriendDialog] ❌ Failed to set avatar - circle or image is null/error");
            }
        } catch (Exception e) {
            System.err.println("[AddFriendDialog] ❌ Error loading avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get status display text
     */
    private String getStatusText(String status) {
        if (status == null) return "⚫ Offline";
        
        return switch (status.toLowerCase()) {
            case "online" -> "🟢 Online";
            case "idle" -> "🟡 Idle";
            case "dnd" -> "🔴 Do Not Disturb";
            default -> "⚫ Offline";
        };
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handle close button
     */
    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
