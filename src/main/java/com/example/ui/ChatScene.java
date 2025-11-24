package com.example.ui;

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
 * Messenger-like dashboard focusing on chat, group chat and online presence.
 * Advanced modules (calls, livestream, AI) are exposed through toolbar buttons
 * but mocked until backend layers (sockets, ORM, Redis) are wired in.
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
    @FXML private Label toolbarHintLabel;
    @FXML private Label currentUserNameLabel;
    @FXML private Label currentUserStatusLabel;
    @FXML private ToggleButton filterDMButton;
    @FXML private ToggleButton filterGroupButton;
    @FXML private ToggleButton filterFavoriteButton;

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
        typingStatusLabel.setText("Ready. Tin nhắn sẽ tự retry nếu chuyển sang LAN mode.");
    }

    private void initProfile() {
        currentUserNameLabel.setText("Minh Anh");
        currentUserStatusLabel.setText("Online • Hybrid P2P");
        connectionStatusLabel.setText("Connected to Central Server");
        encryptionStatusLabel.setText("E2EE Active (RSA + AES)");
        lanModeLabel.setText("Hybrid P2P Mode");
        toolbarHintLabel.setText("Toolbar chứa Friends / Groups / Calls / Livestream / AI.");
    }

    private void initConversations() {
        allConversations.addAll(
            new ConversationItem("Huy Pham", "Direct", true, "Có tối đi call không bro?", true),
            new ConversationItem("UI Design Squad", "Group", false, "Figma file update 1.2", true),
            new ConversationItem("An Nguyen", "Direct", true, "Share giúp mình log socket nha!", false),
            new ConversationItem("Livestream Crew", "Group", true, "Next stream cuối tuần nhé!", true),
            new ConversationItem("AI Research", "Group", false, "Adaptive bitrate báo cáo mới", false)
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
        // Add a tooltip to the search field
        searchField.setTooltip(new Tooltip("Search for friends or groups"));
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
        // Add context menu to online list
        onlineListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewProfile = new MenuItem("View Profile");
            MenuItem sendMessage = new MenuItem("Send Message");
            contextMenu.getItems().addAll(viewProfile, sendMessage);
            cell.textProperty().bind(cell.itemProperty());
            cell.setContextMenu(contextMenu);
            return cell;
        });
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
        filterFavoriteButton.setToggleGroup(filterGroup);
    }

    private void selectDefaultConversation() {
        if (!conversationListView.getItems().isEmpty()) {
            conversationListView.getSelectionModel().select(0);
        } else {
            activeConversationLabel.setText("No conversations");
            activeStatusLabel.setText("Start a new chat to get going.");
        }
    }

    private void loadMessagesFor(ConversationItem conversation) {
        activeConversationLabel.setText(conversation.getName());
        activeStatusLabel.setText(conversation.isOnline()
            ? "Online • P2P ready"
            : "Offline • Tin nhắn sẽ queue chờ LAN");

        ObservableList<MessageItem> messages = FXCollections.observableArrayList(
            new MessageItem(conversation.getName(), "Chào bạn! Dự án hôm nay thế nào?", LocalDateTime.now().minusMinutes(12), false),
            new MessageItem("You", "Đang hoàn thiện UI chat mới giống Messenger.", LocalDateTime.now().minusMinutes(8), true),
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
                    } else if (filterFavoriteButton.isSelected()) {
                        return conv.isFavorite();
                    }
                    return true;
                })
                .toList()
        );

        if (!filteredConversations.contains(activeConversation) && !filteredConversations.isEmpty()) {
            conversationListView.getSelectionModel().select(0);
        }
    }

    // ===== Toolbar Actions =====
    @FXML private void handleToolbarChats() { toolbarHintLabel.setText("Chat mode đang mở."); }
    @FXML private void handleToolbarFriends() { toolbarHintLabel.setText("Friends: quản lý lời mời & danh sách. (Mock)"); }
    @FXML private void handleToolbarGroups() { toolbarHintLabel.setText("Groups: tạo/ quản lý nhóm. (Mock)"); }
    @FXML private void handleToolbarCalls() { toolbarHintLabel.setText("Calls: Voice/Video bằng TCP/UDP thuần. (Mock)"); }
    @FXML private void handleToolbarLivestream() { toolbarHintLabel.setText("Livestream: P2P + adaptive bitrate. (Mock)"); }
    @FXML private void handleToolbarAI() { toolbarHintLabel.setText("AI Assistant: gợi ý bitrate/auto reply. (Mock)"); }
    @FXML private void handleToolbarSettings() { toolbarHintLabel.setText("Settings sẽ bật popup cấu hình hybrid mode. (Mock)"); }

    // ===== Chat Actions =====
    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) {
            typingStatusLabel.setText("Message can't be empty.");
            return;
        }
        if (activeConversation == null) {
            typingStatusLabel.setText("Select a conversation first.");
            return;
        }
        MessageItem newMessage = new MessageItem("You", text.trim(), LocalDateTime.now(), true);
        messageListView.getItems().add(newMessage);
        messageInput.clear();
        typingStatusLabel.setText("Sent at " + timeFormatter.format(newMessage.timestamp()) + ". Nếu server tắt sẽ auto retry.");
        messageListView.scrollTo(messageListView.getItems().size() - 1);
    }

    @FXML
    private void handleInsertEmoji() {
        messageInput.appendText(" 😀 ");
        typingStatusLabel.setText("Emoji inserted.");
    }

    @FXML
    private void handleShareFile() {
        typingStatusLabel.setText("File picker sẽ mở khi backend kết nối. File gửi qua TCP + AES.");
    }

    @FXML
    private void handleStartVoiceCall() {
        typingStatusLabel.setText("Voice call mock — UDP + Virtual Thread sẽ tích hợp sau.");
    }

    @FXML
    private void handleStartVideoCall() {
        typingStatusLabel.setText("Video call mock — WebRTC/ICE4J sẽ gắn sau.");
    }

    @FXML
    private void handleNewChat() {
        typingStatusLabel.setText("New chat dialog coming soon.");
    }

    @FXML
    private void handleChangeStatus() {
        currentUserStatusLabel.setText("Focus • Building socket layer");
        typingStatusLabel.setText("Status updated to Focus.");
    }

    // ===== Filter Buttons =====
    @FXML private void handleFilterDM() { applyFilters(); }
    @FXML private void handleFilterGroup() { applyFilters(); }
    @FXML private void handleFilterFavorite() { applyFilters(); }

    // ===== Helper Classes =====
    private record ConversationItem(String name, String type, boolean online, String lastMessage, boolean favorite) {
        String getName() { return name; }
        String getType() { return type; }
        boolean isOnline() { return online; }
        boolean isFavorite() { return favorite; }
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