package com.example.ui;

import com.example.api.dto.UserDTO;
import com.example.service.FriendService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Create Group Dialog
 */
public class CreateGroupDialog {
    
    @FXML private TextField groupNameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField searchField;
    @FXML private ListView<FriendListItem> friendsListView;
    @FXML private Label selectedCountLabel;
    @FXML private Button createButton;
    
    private final FriendService friendService = new FriendService();
    private final ObservableList<FriendListItem> allFriends = FXCollections.observableArrayList();
    private final Set<Long> selectedFriendIds = new HashSet<>();
    
    private String resultGroupName;
    private String resultDescription;
    private List<Long> resultMemberIds;
    private boolean confirmed = false;
    
    @FXML
    public void initialize() {
        setupFriendsList();
        setupSearchFilter();
        setupValidation();
        loadFriends();
    }
    
    private void setupFriendsList() {
        friendsListView.setItems(allFriends);
        friendsListView.setCellFactory(lv -> new FriendListCell());
        friendsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Handle selection changes
        friendsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectedCount();
        });
    }
    
    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            filterFriends(newText);
        });
    }
    
    private void setupValidation() {
        // Enable/disable create button based on group name
        groupNameField.textProperty().addListener((obs, oldText, newText) -> {
            createButton.setDisable(newText == null || newText.trim().isEmpty());
        });
        
        // Initially disabled
        createButton.setDisable(true);
    }
    
    private void loadFriends() {
        CompletableFuture.runAsync(() -> {
            try {
                List<UserDTO> friends = friendService.getFriendsList();
                
                Platform.runLater(() -> {
                    if (friends != null && !friends.isEmpty()) {
                        for (UserDTO friend : friends) {
                            allFriends.add(new FriendListItem(
                                friend.getId(),
                                friend.getUsername(),
                                friend.getEmail(),
                                friend.getAvatarUrl(),
                                true // TODO: Get actual online status
                            ));
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[CreateGroupDialog] Error loading friends: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void filterFriends(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            friendsListView.setItems(allFriends);
            return;
        }
        
        String lowerSearch = searchText.toLowerCase();
        ObservableList<FriendListItem> filtered = allFriends.filtered(friend -> 
            friend.username.toLowerCase().contains(lowerSearch) ||
            (friend.email != null && friend.email.toLowerCase().contains(lowerSearch))
        );
        
        friendsListView.setItems(filtered);
    }
    
    private void updateSelectedCount() {
        int count = friendsListView.getSelectionModel().getSelectedItems().size();
        selectedCountLabel.setText(count + " được chọn");
    }
    
    @FXML
    private void handleCreate() {
        String groupName = groupNameField.getText().trim();
        
        if (groupName.isEmpty()) {
            showError("Vui lòng nhập tên nhóm!");
            return;
        }
        
        // Get selected members
        List<FriendListItem> selectedItems = friendsListView.getSelectionModel().getSelectedItems();
        List<Long> memberIds = new ArrayList<>();
        for (FriendListItem item : selectedItems) {
            memberIds.add(item.userId);
        }
        
        // Set results
        this.resultGroupName = groupName;
        this.resultDescription = descriptionField.getText().trim();
        this.resultMemberIds = memberIds;
        this.confirmed = true;
        
        // Close dialog
        closeDialog();
    }
    
    @FXML
    private void handleClose() {
        this.confirmed = false;
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) groupNameField.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Getters for results
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getGroupName() {
        return resultGroupName;
    }
    
    public String getDescription() {
        return resultDescription;
    }
    
    public List<Long> getMemberIds() {
        return resultMemberIds;
    }
    
    // Friend List Item
    private static class FriendListItem {
        final Long userId;
        final String username;
        final String email;
        final String avatarUrl;
        final boolean online;
        
        FriendListItem(Long userId, String username, String email, String avatarUrl, boolean online) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.avatarUrl = avatarUrl;
            this.online = online;
        }
    }
    
    // Custom List Cell
    private class FriendListCell extends ListCell<FriendListItem> {
        private final HBox container;
        private final Circle avatar;
        private final VBox infoBox;
        private final Label nameLabel;
        private final Label emailLabel;
        private final Label statusLabel;
        private final CheckBox selectBox;
        
        public FriendListCell() {
            container = new HBox(12);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("friend-cell-container");
            
            // Avatar
            avatar = new Circle(20);
            avatar.getStyleClass().add("friend-avatar");
            avatar.setFill(Color.web("#5865f2"));
            
            // Info
            infoBox = new VBox(4);
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            
            nameLabel = new Label();
            nameLabel.getStyleClass().add("friend-name");
            
            emailLabel = new Label();
            emailLabel.getStyleClass().add("friend-email");
            
            statusLabel = new Label();
            
            infoBox.getChildren().addAll(nameLabel, emailLabel, statusLabel);
            
            // Checkbox
            selectBox = new CheckBox();
            selectBox.getStyleClass().add("select-checkbox");
            
            container.getChildren().addAll(avatar, infoBox, selectBox);
            
            // Handle checkbox changes
            selectBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (getItem() != null) {
                    if (isSelected) {
                        getListView().getSelectionModel().select(getItem());
                    } else {
                        getListView().getSelectionModel().clearSelection(getIndex());
                    }
                }
            });
        }
        
        @Override
        protected void updateItem(FriendListItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                nameLabel.setText(item.username);
                emailLabel.setText(item.email != null ? item.email : "");
                
                if (item.online) {
                    statusLabel.setText("● Online");
                    statusLabel.getStyleClass().setAll("friend-status-online");
                } else {
                    statusLabel.setText("● Offline");
                    statusLabel.getStyleClass().setAll("friend-status-offline");
                }
                
                selectBox.setSelected(isSelected());
                setGraphic(container);
            }
        }
    }
}
