package com.example.ui;

import com.example.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modern Discord-inspired chat interface controller
 * Handles conversation list, messaging, and user interactions
 */
public class ChatScene {

    // ===== FXML Components =====
    @FXML private ListView<ConversationItem> conversationListView;
    @FXML private ListView<MessageItem> messageListView;
    @FXML private ListView<String> onlineListView;
    @FXML private ListView<String> groupOverviewListView;
    @FXML private TextField searchField;
    @FXML private TextArea messageInput;
    @FXML private Label activeConversationLabel;
    @FXML private Label activeStatusLabel;
    @FXML private Label typingStatusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label encryptionStatusLabel;
    @FXML private Label lanModeLabel;
    @FXML private Label currentUserNameLabel;
    @FXML private Label currentUserStatusLabel;
    @FXML private ToggleButton filterDMButton;
    @FXML private ToggleButton filterGroupButton;

    private final ObservableList<ConversationItem> allConversations = FXCollections.observableArrayList();
    private final ObservableList<ConversationItem> filteredConversations = FXCollections.observableArrayList();
    private ConversationItem activeConversation;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        initProfile();
        initConversations();
        initConversationList();
        initMessageView();
        initOnlineList();
        initGroupOverview();
        initFilters();
        selectDefaultConversation();
        typingStatusLabel.setText("Sẵn sàng chat. Tin nhắn sẽ tự động gửi lại nếu chuyển sang LAN mode.");
    }

    private void initProfile() {
        currentUserNameLabel.setText("Minh Anh");
        currentUserStatusLabel.setText("🟢 Online");
        connectionStatusLabel.setText("🟢 Đã kết nối với máy chủ");
        encryptionStatusLabel.setText("🔒 E2EE đang hoạt động");
        lanModeLabel.setText("🌐 Hybrid P2P Mode");
    }

    private void initConversations() {
        allConversations.addAll(
            new ConversationItem("Huy Pham", "Direct", true, "Có tối đi call không bro?"),
            new ConversationItem("UI Design Squad", "Group", false, "Figma file update 1.2"),
            new ConversationItem("An Nguyen", "Direct", true, "Share giúp mình log socket nha!"),
            new ConversationItem("Livestream Crew", "Group", true, "Next stream cuối tuần nhé!"),
            new ConversationItem("AI Research", "Group", false, "Adaptive bitrate báo cáo mới")
        );
        filteredConversations.setAll(allConversations);
    }

    private void initConversationList() {
        conversationListView.setItems(filteredConversations);
        conversationListView.setCellFactory(list -> new ConversationCell());
        conversationListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    activeConversation = newVal;
                    loadMessagesFor(newVal);
                }
            });
        searchField.textProperty().addListener((obs, old, text) -> applyFilters());
        searchField.setTooltip(new Tooltip("Tìm kiếm bạn bè hoặc nhóm"));
    }

    private void initMessageView() {
        messageListView.setCellFactory(list -> new MessageCell());
    }

    private void initOnlineList() {
        onlineListView.setItems(FXCollections.observableArrayList(
            "Huy Pham • Online",
            "An Nguyen • Online",
            "Livestream Crew • 3 online",
            "UI Design Squad • 2 online"
        ));
    }

    private void initGroupOverview() {
        groupOverviewListView.setItems(FXCollections.observableArrayList(
            "UI Design Squad • Admin",
            "Livestream Crew • Host",
            "AI Research • Analyst"
        ));
    }

    private void initFilters() {
        ToggleGroup filterGroup = new ToggleGroup();
        filterDMButton.setToggleGroup(filterGroup);
        filterGroupButton.setToggleGroup(filterGroup);
    }

    private void selectDefaultConversation() {
        if (!conversationListView.getItems().isEmpty()) {
            conversationListView.getSelectionModel().select(0);
        } else {
            activeConversationLabel.setText("# Chưa có cuộc trò chuyện");
            activeStatusLabel.setText("Chọn bạn bè hoặc nhóm để bắt đầu chat");
        }
    }

    private void loadMessagesFor(ConversationItem conversation) {
        activeConversationLabel.setText("# " + conversation.getName());
        activeStatusLabel.setText(conversation.isOnline()
            ? "🟢 Đang hoạt động • P2P sẵn sàng"
            : "⚫ Ngoại tuyến • Tin nhắn sẽ được lưu hàng đợi");

        ObservableList<MessageItem> messages = FXCollections.observableArrayList(
            new MessageItem(conversation.getName(), "Chào bạn! Dự án hôm nay thế nào?", LocalDateTime.now().minusMinutes(12), false),
            new MessageItem("You", "Đang hoàn thiện UI chat mới giống Discord.", LocalDateTime.now().minusMinutes(8), true),
            new MessageItem(conversation.getName(), "Nice! Nhớ test fallback LAN nhé.", LocalDateTime.now().minusMinutes(4), false)
        );
        messageListView.setItems(messages);
        messageListView.scrollTo(messages.size() - 1);
    }

    private void applyFilters() {
        String keyword = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        filteredConversations.setAll(
            allConversations.stream()
                .filter(conv -> conv.getName().toLowerCase().contains(keyword))
                .filter(conv -> {
                    if (filterDMButton.isSelected()) {
                        return "Direct".equals(conv.getType());
                    } else if (filterGroupButton.isSelected()) {
                        return "Group".equals(conv.getType());
                    }
                    return true;
                })
                .toList()
        );

        if (!filteredConversations.contains(activeConversation) && !filteredConversations.isEmpty()) {
            conversationListView.getSelectionModel().select(0);
        }
    }





    @FXML
    private void handleOpenProfile() {
        // Navigate to user profile page
        SceneManager.setTitle("Discord Mini - Trang Cá Nhân");
        SceneManager.loadContent("content/profile-content.fxml");
    }

    // ===== Chat Header Actions =====
    @FXML
    private void handleStartCall() {
        if (activeConversation == null) {
            typingStatusLabel.setText("Vui lòng chọn cuộc trò chuyện trước.");
            return;
        }
        String callType = "Direct".equals(activeConversation.getType()) ? "1-1" : "nhóm";
        typingStatusLabel.setText("Gọi điện " + callType + " - tính năng đang phát triển.");
    }

    @FXML
    private void handleShowInfo() {
        if (activeConversation == null) {
            typingStatusLabel.setText("Vui lòng chọn cuộc trò chuyện trước.");
            return;
        }
        String infoType = "Direct".equals(activeConversation.getType()) ? "người dùng" : "nhóm";
        typingStatusLabel.setText("Xem thông tin " + infoType + " - tính năng đang phát triển.");
    }

    @FXML
    private void handleAttachment() {
        typingStatusLabel.setText("Đính kèm file - tính năng đang phát triển.");
    }

    // ===== Chat Actions =====
    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) {
            typingStatusLabel.setText("Tin nhắn không được để trống.");
            return;
        }
        if (activeConversation == null) {
            typingStatusLabel.setText("Vui lòng chọn cuộc trò chuyện trước.");
            return;
        }
        MessageItem newMessage = new MessageItem("You", text.trim(), LocalDateTime.now(), true);
        messageListView.getItems().add(newMessage);
        messageInput.clear();
        typingStatusLabel.setText("Đã gửi lúc " + timeFormatter.format(newMessage.timestamp()) + ". Nếu server tắt sẽ tự động gửi lại.");
        messageListView.scrollTo(messageListView.getItems().size() - 1);
    }

    @FXML
    private void handleInsertEmoji() {
        messageInput.appendText(" 😀 ");
        typingStatusLabel.setText("Đã thêm emoji.");
    }

    // ===== Filter Buttons =====
    @FXML private void handleFilterDM() { applyFilters(); }
    @FXML private void handleFilterGroup() { applyFilters(); }

    // ===== Helper Classes =====
    private record ConversationItem(String name, String type, boolean online, String lastMessage) {
        String getName() { return name; }
        String getType() { return type; }
        boolean isOnline() { return online; }
        String lastMessagePreview() { return lastMessage; }
    }

    private record MessageItem(String author, String content, LocalDateTime timestamp, boolean own) { }

    private class ConversationCell extends ListCell<ConversationItem> {
        private final VBox container = new VBox(4);
        private final HBox header = new HBox(6);
        private final Label nameLabel = new Label();
        private final Label statusChip = new Label();
        private final Label previewLabel = new Label();

        ConversationCell() {
            nameLabel.getStyleClass().add("conversation-name");
            statusChip.getStyleClass().add("status-chip");
            previewLabel.getStyleClass().add("conversation-preview");
            header.getChildren().addAll(nameLabel, statusChip);
            container.getChildren().addAll(header, previewLabel);
            container.getStyleClass().add("conversation-cell");
        }

        @Override
        protected void updateItem(ConversationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                statusChip.setText(item.getType());
                statusChip.getStyleClass().removeAll("online", "offline");
                statusChip.getStyleClass().add(item.isOnline() ? "online" : "offline");
                previewLabel.setText(item.lastMessagePreview());
                setGraphic(container);
            }
        }
    }

    private class MessageCell extends ListCell<MessageItem> {
        private final HBox container = new HBox();
        private final VBox bubble = new VBox(4);
        private final Label authorLabel = new Label();
        private final Label contentLabel = new Label();
        private final Label timeLabel = new Label();

        MessageCell() {
            container.getChildren().add(bubble);
            HBox.setHgrow(bubble, Priority.ALWAYS);
            contentLabel.setWrapText(true);
            authorLabel.getStyleClass().add("message-author");
            contentLabel.getStyleClass().add("message-content");
            timeLabel.getStyleClass().add("message-time");
            bubble.getChildren().addAll(authorLabel, contentLabel, timeLabel);
            bubble.getStyleClass().add("message-bubble");
        }

        @Override
        protected void updateItem(MessageItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                authorLabel.setText(item.author());
                contentLabel.setText(item.content());
                timeLabel.setText(timeFormatter.format(item.timestamp()));
                boolean own = item.own();
                container.setAlignment(own ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
                bubble.getStyleClass().removeAll("own-message", "other-message");
                bubble.getStyleClass().add(own ? "own-message" : "other-message");
                setGraphic(container);
            }
        }
    }
}
