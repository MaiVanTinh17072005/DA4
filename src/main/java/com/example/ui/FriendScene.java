package com.example.ui;

import com.example.api.dto.UserDTO;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Friends management interface controller
 * Handles friend list, friend requests, and friend details
 */
public class FriendScene {

    // ===== FXML Components =====
    @FXML private ListView<FriendItem> friendsListView;
    @FXML private TextField searchField;
    @FXML private Label currentUserNameLabel;
    @FXML private Label currentUserStatusLabel;
    @FXML private Circle userAvatarCircle;
    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubtitleLabel;
    @FXML private ToggleButton filterAllButton;
    @FXML private ToggleButton filterOnlineButton;
    @FXML private ToggleButton filterPendingButton;
    @FXML private Button addFriendButton;
    
    // Main content sections
    @FXML private VBox mainContentArea;
    @FXML private VBox friendRequestsSection;
    @FXML private VBox friendRequestsContainer;
    @FXML private VBox friendDetailsSection;
    @FXML private VBox emptyStateSection;
    
    // Friend detail components
    @FXML private Circle detailAvatar;
    @FXML private Label detailName;
    @FXML private Label detailUsername;
    @FXML private Label detailStatus;
    @FXML private Label friendSinceLabel;
    @FXML private Label mutualGroupsLabel;
    @FXML private Label emptyStateTitle;
    @FXML private Label emptyStateSubtitle;

    private final ObservableList<FriendItem> allFriends = FXCollections.observableArrayList();
    private final ObservableList<FriendItem> filteredFriends = FXCollections.observableArrayList();
    private final ObservableList<FriendRequest> pendingRequests = FXCollections.observableArrayList();
    private FriendItem selectedFriend;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        initProfile();
        initFriends();
        initFriendRequests();
        initFriendsList();
        initFilters();
        showEmptyState();
    }

    private void initProfile() {
        // Load user data from SessionManager
        UserDTO currentUser = SessionManager.getCurrentUser();
        
        if (currentUser != null) {
            currentUserNameLabel.setText(currentUser.getUsername());
            
            // Map status to display format
            String status = currentUser.getStatus();
            String statusDisplay = switch (status != null ? status.toLowerCase() : "online") {
                case "online" -> "🟢 Online";
                case "idle" -> "🟡 Idle";
                case "dnd", "do_not_disturb" -> "🔴 Do Not Disturb";
                case "offline" -> "⚫ Offline";
                default -> "🟢 Online";
            };
            currentUserStatusLabel.setText(statusDisplay);
            
            // Load avatar
            loadUserAvatar(currentUser.getAvatarUrl());
            
            System.out.println("[FriendScene] Loaded user profile: " + currentUser.getUsername());
        } else {
            // Fallback to defaults
            currentUserNameLabel.setText("Guest");
            currentUserStatusLabel.setText("⚫ Offline");
            loadUserAvatar(null);
            System.out.println("[FriendScene] ⚠ No user session found, using default profile");
        }
    }
    
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
                    System.out.println("[FriendScene] ✓ Loaded user avatar: " + avatarUrl);
                } catch (Exception e) {
                    System.out.println("[FriendScene] ⚠ Failed to load avatar, using default: " + e.getMessage());
                    avatarImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                }
            } else {
                // Load default avatar
                avatarImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                System.out.println("[FriendScene] Using default avatar");
            }
            
            if (userAvatarCircle != null && avatarImage != null && !avatarImage.isError()) {
                userAvatarCircle.setFill(new javafx.scene.paint.ImagePattern(avatarImage));
            }
        } catch (Exception e) {
            System.out.println("[FriendScene] ❌ Error loading avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void initFriends() {
        allFriends.addAll(
            new FriendItem("Huy Pham", "@huypham", "Online", LocalDate.of(2023, 5, 15), 2),
            new FriendItem("An Nguyen", "@annguyen", "Online", LocalDate.of(2023, 8, 20), 1),
            new FriendItem("Minh Tran", "@minhtran", "Offline", LocalDate.of(2024, 1, 10), 3),
            new FriendItem("Linh Vo", "@linhvo", "Online", LocalDate.of(2023, 12, 5), 2),
            new FriendItem("Khoa Le", "@khoale", "Idle", LocalDate.of(2024, 2, 14), 1),
            new FriendItem("Tuan Nguyen", "@tuannguyen", "Offline", LocalDate.of(2023, 9, 30), 4)
        );
        filteredFriends.setAll(allFriends);
    }

    private void initFriendRequests() {
        pendingRequests.addAll(
            new FriendRequest("Nam Pham", "@nampham", "Chào bạn! Mình thấy bạn trong nhóm UI Design.", "2 giờ trước"),
            new FriendRequest("Thao Nguyen", "@thaonguyen", "Hi! Kết bạn nhé!", "1 ngày trước")
        );
    }

    private void initFriendsList() {
        friendsListView.setItems(filteredFriends);
        friendsListView.setCellFactory(list -> new FriendCell());
        friendsListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedFriend = newVal;
                    showFriendDetails(newVal);
                }
            });
        searchField.textProperty().addListener((obs, old, text) -> applyFilters());
        searchField.setTooltip(new Tooltip("Tìm kiếm bạn bè theo tên hoặc username"));
    }

    private void initFilters() {
        ToggleGroup filterGroup = new ToggleGroup();
        filterAllButton.setToggleGroup(filterGroup);
        filterOnlineButton.setToggleGroup(filterGroup);
        filterPendingButton.setToggleGroup(filterGroup);
    }

    private void applyFilters() {
        String keyword = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        filteredFriends.setAll(
            allFriends.stream()
                .filter(friend -> friend.getName().toLowerCase().contains(keyword) 
                               || friend.getUsername().toLowerCase().contains(keyword))
                .filter(friend -> {
                    if (filterOnlineButton.isSelected()) {
                        return "Online".equals(friend.getStatus());
                    } else if (filterPendingButton.isSelected()) {
                        return false; // Will show pending requests instead
                    }
                    return true;
                })
                .toList()
        );

        if (filterPendingButton.isSelected()) {
            showPendingRequests();
        } else if (!filteredFriends.contains(selectedFriend)) {
            showEmptyState();
        }
    }

    private void showEmptyState() {
        friendDetailsSection.setVisible(false);
        friendDetailsSection.setManaged(false);
        friendRequestsSection.setVisible(false);
        friendRequestsSection.setManaged(false);
        emptyStateSection.setVisible(true);
        emptyStateSection.setManaged(true);
        
        emptyStateTitle.setText("Chọn một bạn bè");
        emptyStateSubtitle.setText("Chọn một người bạn từ danh sách để xem thông tin chi tiết");
    }

    private void showFriendDetails(FriendItem friend) {
        emptyStateSection.setVisible(false);
        emptyStateSection.setManaged(false);
        friendRequestsSection.setVisible(false);
        friendRequestsSection.setManaged(false);
        friendDetailsSection.setVisible(true);
        friendDetailsSection.setManaged(true);

        detailName.setText(friend.getName());
        detailUsername.setText(friend.getUsername());
        
        String statusEmoji = switch (friend.getStatus()) {
            case "Online" -> "🟢";
            case "Idle" -> "🟡";
            case "DND" -> "🔴";
            default -> "⚫";
        };
        detailStatus.setText(statusEmoji + " " + friend.getStatus());
        detailStatus.getStyleClass().removeAll("online", "offline");
        detailStatus.getStyleClass().add(friend.getStatus().toLowerCase());
        
        friendSinceLabel.setText(dateFormatter.format(friend.getFriendSince()));
        mutualGroupsLabel.setText(friend.getMutualGroups() + " nhóm");
    }

    private void showPendingRequests() {
        friendDetailsSection.setVisible(false);
        friendDetailsSection.setManaged(false);
        emptyStateSection.setVisible(false);
        emptyStateSection.setManaged(false);
        friendRequestsSection.setVisible(true);
        friendRequestsSection.setManaged(true);

        friendRequestsContainer.getChildren().clear();
        
        if (pendingRequests.isEmpty()) {
            VBox emptyBox = new VBox(16);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.getStyleClass().add("empty-state");
            
            Label icon = new Label("✉️");
            icon.getStyleClass().add("empty-state-icon");
            
            Label title = new Label("Không có yêu cầu kết bạn");
            title.getStyleClass().add("empty-state-title");
            
            Label subtitle = new Label("Bạn không có yêu cầu kết bạn nào đang chờ xử lý");
            subtitle.getStyleClass().add("empty-state-subtitle");
            subtitle.setMaxWidth(300);
            subtitle.setWrapText(true);
            
            emptyBox.getChildren().addAll(icon, title, subtitle);
            friendRequestsContainer.getChildren().add(emptyBox);
        } else {
            for (FriendRequest request : pendingRequests) {
                friendRequestsContainer.getChildren().add(createRequestCard(request));
            }
        }
    }

    private VBox createRequestCard(FriendRequest request) {
        VBox card = new VBox(12);
        card.getStyleClass().add("friend-request-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(24);
        avatar.getStyleClass().add("friend-avatar");

        VBox info = new VBox(4);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label name = new Label(request.getName());
        name.getStyleClass().add("friend-request-name");

        Label username = new Label(request.getUsername());
        username.getStyleClass().add("friend-detail-username");

        info.getChildren().addAll(name, username);

        Label time = new Label(request.getTime());
        time.getStyleClass().add("friend-request-time");

        header.getChildren().addAll(avatar, info, time);

        Label message = new Label(request.getMessage());
        message.getStyleClass().add("friend-request-message");
        message.setWrapText(true);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("✓ Chấp nhận");
        acceptBtn.getStyleClass().add("accept-btn");
        acceptBtn.setOnAction(e -> handleAcceptRequest(request));

        Button declineBtn = new Button("✗ Từ chối");
        declineBtn.getStyleClass().add("decline-btn");
        declineBtn.setOnAction(e -> handleDeclineRequest(request));

        actions.getChildren().addAll(acceptBtn, declineBtn);

        card.getChildren().addAll(header, message, actions);
        return card;
    }

    // ===== Filter Actions =====
    @FXML
    private void handleFilterAll() {
        applyFilters();
    }

    @FXML
    private void handleFilterOnline() {
        applyFilters();
    }

    @FXML
    private void handleFilterPending() {
        applyFilters();
    }

    // ===== Friend Actions =====
    @FXML
    private void handleAddFriend() {
        try {
            // Load the Add Friend dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/fxml/add-friend-dialog.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // Get the controller and set the stage
            AddFriendDialog controller = loader.getController();
            
            // Create a new stage for the dialog
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Thêm Bạn Bè");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogStage.setScene(new javafx.scene.Scene(root));
            
            // Set the stage in the controller
            controller.setDialogStage(dialogStage);
            
            // Show the dialog
            dialogStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở cửa sổ thêm bạn: " + e.getMessage());
            alert.showAndWait();
        }
    }


    @FXML
    private void handleSendMessage() {
        if (selectedFriend != null) {
            // Navigate to chat with this friend
            SceneManager.setTitle("Discord Mini - Chat");
            SceneManager.loadContent("content/chat-content.fxml");
        }
    }

    @FXML
    private void handleCall() {
        if (selectedFriend != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gọi điện");
            alert.setHeaderText(null);
            alert.setContentText("Đang gọi " + selectedFriend.getName() + "...");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleRemoveFriend() {
        if (selectedFriend != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận");
            confirm.setHeaderText("Xóa bạn bè");
            confirm.setContentText("Bạn có chắc muốn xóa " + selectedFriend.getName() + " khỏi danh sách bạn bè?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    allFriends.remove(selectedFriend);
                    filteredFriends.remove(selectedFriend);
                    showEmptyState();
                }
            });
        }
    }

    private void handleAcceptRequest(FriendRequest request) {
        pendingRequests.remove(request);
        allFriends.add(new FriendItem(
            request.getName(), 
            request.getUsername(), 
            "Offline", 
            LocalDate.now(), 
            0
        ));
        showPendingRequests();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText("Đã chấp nhận lời mời kết bạn từ " + request.getName());
        alert.showAndWait();
    }

    private void handleDeclineRequest(FriendRequest request) {
        pendingRequests.remove(request);
        showPendingRequests();
    }

    @FXML
    private void handleOpenProfile() {
        SceneManager.setTitle("Discord Mini - Trang Cá Nhân");
        SceneManager.loadContent("content/profile-content.fxml");
    }

    // ===== Helper Classes =====
    private record FriendItem(
        String name, 
        String username, 
        String status, 
        LocalDate friendSince, 
        int mutualGroups
    ) {
        String getName() { return name; }
        String getUsername() { return username; }
        String getStatus() { return status; }
        LocalDate getFriendSince() { return friendSince; }
        int getMutualGroups() { return mutualGroups; }
    }

    private record FriendRequest(
        String name, 
        String username, 
        String message, 
        String time
    ) {
        String getName() { return name; }
        String getUsername() { return username; }
        String getMessage() { return message; }
        String getTime() { return time; }
    }

    private class FriendCell extends ListCell<FriendItem> {
        private final HBox container = new HBox(12);
        private final Circle avatar = new Circle(20);
        private final VBox info = new VBox(4);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Circle statusIndicator = new Circle(6);

        FriendCell() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("friend-cell");
            
            avatar.getStyleClass().add("friend-avatar");
            nameLabel.getStyleClass().add("friend-name");
            statusLabel.getStyleClass().add("friend-status");
            statusIndicator.getStyleClass().add("status-indicator");
            
            info.getChildren().addAll(nameLabel, statusLabel);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
            container.getChildren().addAll(avatar, info, statusIndicator);
        }

        @Override
        protected void updateItem(FriendItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                statusLabel.setText(item.getStatus());
                statusLabel.getStyleClass().removeAll("online", "offline");
                statusLabel.getStyleClass().add(item.getStatus().toLowerCase());
                
                statusIndicator.getStyleClass().removeAll("online", "offline", "idle", "dnd");
                statusIndicator.getStyleClass().add(item.getStatus().toLowerCase());
                
                setGraphic(container);
            }
        }
    }
}
