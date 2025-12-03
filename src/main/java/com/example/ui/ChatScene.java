package com.example.ui;

import com.example.api.dto.MessageDTO;
import com.example.api.dto.UserDTO;
import com.example.network.P2PManager;
import com.example.network.P2PMessageListener;
import com.example.network.PeerManager;
import com.example.network.model.P2PMessage;
import com.example.service.FriendService;
import com.example.service.MessageService;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Modern Discord-inspired chat interface controller
 * Handles conversation list, messaging, and user interactions
 */
public class ChatScene implements P2PMessageListener {

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
    @FXML private Circle userAvatarCircle;
    @FXML private ToggleButton filterDMButton;
    @FXML private ToggleButton filterGroupButton;

    private final ObservableList<ConversationItem> allConversations = FXCollections.observableArrayList();
    private final ObservableList<ConversationItem> filteredConversations = FXCollections.observableArrayList();
    private ConversationItem activeConversation;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    // Services
    private final FriendService friendService = new FriendService();
    private final MessageService messageService = new MessageService();
    
    // P2P Manager
    private P2PManager p2pManager;
    private boolean p2pEnabled = false;

    @FXML
    public void initialize() {
        initProfile();
        initP2P();
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
            
            System.out.println("[ChatScene] Loaded user profile: " + currentUser.getUsername());
        } else {
            // Fallback to defaults
            currentUserNameLabel.setText("Guest");
            currentUserStatusLabel.setText("⚫ Offline");
            loadUserAvatar(null);
            System.out.println("[ChatScene] ⚠ No user session found, using default profile");
        }
        
        connectionStatusLabel.setText("🟢 Đã kết nối với máy chủ");
        encryptionStatusLabel.setText("🔒 E2EE đang hoạt động");
        lanModeLabel.setText("🌐 Hybrid P2P Mode");
    }
    
    private void loadUserAvatar(String avatarUrl) {
        try {
            Image avatarImage;
            
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                // Try to load user's avatar
                try {
                    avatarImage = new Image(getClass().getResourceAsStream(avatarUrl));
                    if (avatarImage.isError()) {
                        throw new Exception("Failed to load avatar from: " + avatarUrl);
                    }
                    System.out.println("[ChatScene] ✓ Loaded user avatar: " + avatarUrl);
                } catch (Exception e) {
                    System.out.println("[ChatScene] ⚠ Failed to load avatar, using default: " + e.getMessage());
                    avatarImage = new Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                }
            } else {
                // Load default avatar
                avatarImage = new Image(getClass().getResourceAsStream("/com/example/images/macdinh.jpg"));
                System.out.println("[ChatScene] Using default avatar");
            }
            
            if (userAvatarCircle != null && avatarImage != null && !avatarImage.isError()) {
                userAvatarCircle.setFill(new ImagePattern(avatarImage));
            }
        } catch (Exception e) {
            System.out.println("[ChatScene] ❌ Error loading avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initConversations() {
        // Load real friends from API
        System.out.println("[ChatScene] Loading friends from API...");
        
        try {
            List<UserDTO> friends = friendService.getFriendsList();
            
            if (friends != null && !friends.isEmpty()) {
                System.out.println("[ChatScene] ✅ Loaded " + friends.size() + " friend(s)");
                
                for (UserDTO friend : friends) {
                    // Determine online status
                    boolean isOnline = "online".equalsIgnoreCase(friend.getStatus());
                    
                    // Create conversation item
                    ConversationItem item = new ConversationItem(
                        friend.getUsername(),
                        "Direct",
                        isOnline,
                        "Bắt đầu trò chuyện...",
                        friend.getId()
                    );
                    allConversations.add(item);
                }
            } else {
                System.out.println("[ChatScene] ⚠️ No friends found");
            }
            
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Error loading friends: " + e.getMessage());
            e.printStackTrace();
        }
        
        filteredConversations.setAll(allConversations);
        
        // Auto-connect to all friends for real-time status updates (UDP heartbeat)
        if (p2pEnabled && p2pManager != null && !allConversations.isEmpty()) {
            System.out.println("[ChatScene] 🔄 Auto-connecting to friends for status updates...");
            autoConnectToFriends();
        }
    }
    
    /**
     * Auto-connect to all friends to receive real-time online/offline status via UDP heartbeat
     */
    private void autoConnectToFriends() {
        CompletableFuture.runAsync(() -> {
            com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
            
            for (ConversationItem conversation : allConversations) {
                try {
                    Long friendId = conversation.getUserId();
                    
                    // Try to get friend's P2P info
                    com.example.api.dto.P2PInfoRequest friendP2PInfo = p2pService.getPeerInfo(friendId);
                    
                    if (friendP2PInfo != null) {
                        // Get friend data
                        List<UserDTO> friends = friendService.getFriendsList();
                        UserDTO friend = friends.stream()
                            .filter(f -> f.getId().equals(friendId))
                            .findFirst()
                            .orElse(null);
                        
                        if (friend != null) {
                            // Connect to friend (will start UDP heartbeat)
                            boolean connected = p2pManager.connectToFriend(
                                friend,
                                friendP2PInfo.getIpAddress(),
                                friendP2PInfo.getTcpPort(),
                                friendP2PInfo.getUdpPort()
                            );
                            
                            if (connected) {
                                System.out.println("[ChatScene] ✅ Auto-connected to " + friend.getUsername() + " for status updates");
                            }
                        }
                    }
                } catch (Exception e) {
                    // Friend offline or not available - skip
                    System.out.println("[ChatScene] ⚠️ " + conversation.getName() + " offline");
                }
            }
        });
    }

    private void initConversationList() {
        conversationListView.setItems(filteredConversations);
        conversationListView.setCellFactory(list -> new ConversationCell());
        conversationListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    activeConversation = newVal;
                    loadMessagesFor(newVal);
                    
                    // Connect to peer for real-time P2P chat
                    if (p2pEnabled && "Direct".equals(newVal.getType())) {
                        System.out.println("[ChatScene] 🔗 Attempting P2P connection to: " + newVal.getName());
                        connectToActivePeer();
                    }
                }
            });
        searchField.textProperty().addListener((obs, old, text) -> applyFilters());
        searchField.setTooltip(new Tooltip("Tìm kiếm bạn bè hoặc nhóm"));
    }

    private void initMessageView() {
        messageListView.setCellFactory(list -> new MessageCell());
    }

    private void initOnlineList() {
        // Load online friends from API
        try {
            List<UserDTO> friends = friendService.getFriendsList();
            List<String> onlineFriends = new ArrayList<>();
            
            if (friends != null) {
                for (UserDTO friend : friends) {
                    if ("online".equalsIgnoreCase(friend.getStatus())) {
                        onlineFriends.add(friend.getUsername() + " • Online");
                    }
                }
            }
            
            if (onlineFriends.isEmpty()) {
                onlineFriends.add("Không có bạn bè online");
            }
            
            onlineListView.setItems(FXCollections.observableArrayList(onlineFriends));
        } catch (Exception e) {
            System.err.println("[ChatScene] Error loading online list: " + e.getMessage());
            onlineListView.setItems(FXCollections.observableArrayList("Không thể tải danh sách"));
        }
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

        // Load real messages from API
        System.out.println("[ChatScene] Loading messages for: " + conversation.getName());
        
        try {
            Long friendId = conversation.getUserId();
            List<MessageDTO> apiMessages = messageService.getConversation(friendId);
            
            ObservableList<MessageItem> messages = FXCollections.observableArrayList();
            
            if (apiMessages != null && !apiMessages.isEmpty()) {
                System.out.println("[ChatScene] ✅ Loaded " + apiMessages.size() + " message(s)");
                
                Long currentUserId = SessionManager.getCurrentUser().getId();
                
                for (MessageDTO msg : apiMessages) {
                    boolean isOwn = msg.getSenderId().equals(currentUserId);
                    String author = isOwn ? "You" : msg.getSenderUsername();
                    
                    // Parse timestamp
                    LocalDateTime timestamp;
                    try {
                        timestamp = LocalDateTime.parse(msg.getTimestamp());
                    } catch (Exception e) {
                        timestamp = LocalDateTime.now();
                    }
                    
                    messages.add(new MessageItem(author, msg.getContent(), timestamp, isOwn));
                }
            } else {
                System.out.println("[ChatScene] ℹ️ No messages found - new conversation");
                // Empty conversation - no messages to show
            }
            
            messageListView.setItems(messages);
            if (!messages.isEmpty()) {
                messageListView.scrollTo(messages.size() - 1);
            }
            
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Error loading messages: " + e.getMessage());
            e.printStackTrace();
            messageListView.setItems(FXCollections.observableArrayList());
        }
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





    
    // ===== P2P Methods =====
    
    private void initP2P() {
        try {
            UserDTO currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                System.err.println("[ChatScene] ❌ No user session, P2P disabled");
                return;
            }
            
            // Get P2P ports from SessionManager (set during login)
            int tcpPort = SessionManager.getTcpPort();
            int udpPort = SessionManager.getUdpPort();
            
            if (tcpPort == 0 || udpPort == 0) {
                System.err.println("[ChatScene] ❌ P2P ports not allocated");
                p2pEnabled = false;
                updateConnectionStatus("🔴 P2P không khả dụng");
                return;
            }
            
            System.out.println("[ChatScene] Initializing P2P with TCP:" + tcpPort + " UDP:" + udpPort);
            
            // Get singleton P2P Manager instance
            p2pManager = P2PManager.getInstance();
            p2pManager.setMessageListener(this);
            
            // Initialize (will only happen once)
            p2pManager.initializeForUser(currentUser.getId(), tcpPort, udpPort);
            
            p2pEnabled = true;
            
            System.out.println("[ChatScene] ✅ P2P initialized");
            updateConnectionStatus("🟢 P2P sẵn sàng");
            
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Failed to initialize P2P: " + e.getMessage());
            e.printStackTrace();
            p2pEnabled = false;
            updateConnectionStatus("🔴 P2P không khả dụng");
        }
    }
    
    private void connectToActivePeer() {
        if (!p2pEnabled || activeConversation == null) {
            return;
        }
        
        // Run in background thread to avoid blocking UI
        CompletableFuture.runAsync(() -> {
            try {
                Long friendId = activeConversation.getUserId();
                
                // Check if already connected
                if (p2pManager.isConnectedToFriend(friendId)) {
                    System.out.println("[ChatScene] ✅ Already connected to peer: " + friendId);
                    Platform.runLater(() -> updateConnectionStatus("🟢 P2P kết nối - E2EE hoạt động"));
                    return;
                }
                
                System.out.println("[ChatScene] 🔍 Fetching P2P info for friend: " + friendId);
                
                // Get friend's P2P info from server
                com.example.service.P2PService p2pService = com.example.service.P2PService.getInstance();
                com.example.api.dto.P2PInfoRequest friendP2PInfo = p2pService.getPeerInfo(friendId);
                
                if (friendP2PInfo != null) {
                    // Get friend data
                    List<UserDTO> friends = friendService.getFriendsList();
                    UserDTO friend = friends.stream()
                        .filter(f -> f.getId().equals(friendId))
                        .findFirst()
                        .orElse(null);
                    
                    if (friend != null) {
                        System.out.println("[ChatScene] 🔗 Connecting to peer " + friend.getUsername() + 
                            " at " + friendP2PInfo.getIpAddress() + ":" + friendP2PInfo.getTcpPort());
                        
                        boolean connected = p2pManager.connectToFriend(
                            friend,
                            friendP2PInfo.getIpAddress(),
                            friendP2PInfo.getTcpPort(),
                            friendP2PInfo.getUdpPort()
                        );
                        
                        if (connected) {
                            Platform.runLater(() -> {
                                updateConnectionStatus("🟢 P2P kết nối - E2EE hoạt động");
                                encryptionStatusLabel.setText("🔒 E2EE đang hoạt động");
                            });
                            System.out.println("[ChatScene] ✅ P2P connected - real-time chat enabled!");
                        } else {
                            Platform.runLater(() -> updateConnectionStatus("🟡 Đang kết nối P2P..."));
                        }
                    }
                } else {
                    System.out.println("[ChatScene] ⚠️ Friend P2P info not available, using server mode");
                    Platform.runLater(() -> updateConnectionStatus("🌐 Server mode"));
                }
                
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Error connecting to peer: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> updateConnectionStatus("🔴 Lỗi kết nối P2P"));
            }
        });
    }
    
    private void updateConnectionStatus(String status) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText(status);
        });
    }
    
    // ===== P2PMessageListener Implementation =====
    
    @Override
    public void onMessageReceived(P2PMessage message) {
        System.out.println("[ChatScene] 📨 P2P message received: " + message.getContent());
        
        Platform.runLater(() -> {
            // Check if message is from active conversation
            if (activeConversation != null && message.getSenderId().equals(activeConversation.getUserId())) {
                // Add message to UI
                LocalDateTime timestamp = LocalDateTime.ofEpochSecond(
                    message.getTimestamp() / 1000, 
                    0, 
                    java.time.ZoneOffset.UTC
                );
                
                MessageItem item = new MessageItem(
                    message.getSenderUsername() != null ? message.getSenderUsername() : "Friend",
                    message.getContent(),
                    timestamp,
                    false
                );
                
                messageListView.getItems().add(item);
                messageListView.scrollTo(messageListView.getItems().size() - 1);
                
                typingStatusLabel.setText("Tin nhắn mới từ " + message.getSenderUsername());
            } else {
                // Message from other conversation, show notification
                typingStatusLabel.setText("Tin nhắn mới từ " + message.getSenderUsername());
            }
        });
    }
    
    @Override
    public void onPeerConnected(Long peerId, String peerUsername) {
        System.out.println("[ChatScene] ✅ Peer connected: " + peerId);
        
        Platform.runLater(() -> {
            // Update active conversation status
            if (activeConversation != null && peerId.equals(activeConversation.getUserId())) {
                updateConnectionStatus("🟢 P2P kết nối - E2EE hoạt động");
                activeStatusLabel.setText("🟢 Đang hoạt động • P2P sẵn sàng");
            }
            
            // Update conversation list - mark as online
            updateConversationStatus(peerId, true);
        });
    }
    
    @Override
    public void onPeerDisconnected(Long peerId) {
        System.out.println("[ChatScene] 🔌 Peer disconnected: " + peerId);
        
        Platform.runLater(() -> {
            // Update active conversation status
            if (activeConversation != null && peerId.equals(activeConversation.getUserId())) {
                updateConnectionStatus("🔴 Peer offline");
                activeStatusLabel.setText("⚫ Ngoại tuyến • Tin nhắn sẽ được lưu hàng đợi");
            }
            
            // Update conversation list - mark as offline
            updateConversationStatus(peerId, false);
        });
    }
    
    /**
     * Update online/offline status in conversation list (real-time via UDP heartbeat)
     */
    private void updateConversationStatus(Long userId, boolean online) {
        // Find conversation in list and update status
        for (int i = 0; i < conversationListView.getItems().size(); i++) {
            ConversationItem item = conversationListView.getItems().get(i);
            if (item.getUserId().equals(userId)) {
                // Create updated item with new status
                ConversationItem updated = new ConversationItem(
                    item.getName(),
                    item.getType(),
                    online,  // Update online status
                    item.lastMessagePreview(),
                    item.getUserId()
                );
                conversationListView.getItems().set(i, updated);
                System.out.println("[ChatScene] 🔄 Updated status for " + item.getName() + ": " + (online ? "ONLINE" : "OFFLINE"));
                break;
            }
        }
    }
    
    
    @Override
    public void onConnectionError(Long peerId, Exception e) {
        System.err.println("[ChatScene] ❌ Connection error: " + e.getMessage());
        
        Platform.runLater(() -> {
            updateConnectionStatus("🔴 Lỗi kết nối");
            typingStatusLabel.setText("Lỗi kết nối P2P, chuyển sang server mode");
        });
    }
    
    @Override
    public void onTypingIndicator(Long peerId, boolean isTyping) {
        Platform.runLater(() -> {
            if (activeConversation != null && peerId.equals(activeConversation.getUserId())) {
                if (isTyping) {
                    typingStatusLabel.setText(activeConversation.getName() + " đang nhập...");
                } else {
                    typingStatusLabel.setText("");
                }
            }
        });
    }
    
    @Override
    public void onMessageAcknowledged(String messageId) {
        System.out.println("[ChatScene] ✅ Message delivered: " + messageId);
        
        Platform.runLater(() -> {
            typingStatusLabel.setText("✓ Đã gửi");
        });
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
        
        UserDTO currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        
        Long friendId = activeConversation.getUserId();
        String content = text.trim();
        
        // Add message to UI immediately
        MessageItem newMessage = new MessageItem(
            currentUser.getUsername(),
            content,
            LocalDateTime.now(),
            true
        );
        messageListView.getItems().add(newMessage);
        messageInput.clear();
        scrollToBottom();
        
        // Try P2P first
        if (p2pEnabled && p2pManager != null && p2pManager.isConnectedToFriend(friendId)) {
            System.out.println("[ChatScene] 📤 Sending via P2P");
            
            p2pManager.sendTextMessage(friendId, content).thenAccept(success -> {
                if (success) {
                    System.out.println("[ChatScene] ✅ P2P message sent");
                    
                    // Persist to database (async)
                    CompletableFuture.runAsync(() -> {
                        try {
                            MessageDTO messageDTO = new MessageDTO();
                            messageDTO.setReceiverId(friendId);
                            messageDTO.setContent(content);
                            messageDTO.setMsgType("text");
                            messageService.sendMessage(messageDTO);
                            System.out.println("[ChatScene] ✅ Message persisted to database");
                            Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi qua P2P (E2EE)"));
                        } catch (Exception e) {
                            System.err.println("[ChatScene] ⚠️ Failed to persist: " + e.getMessage());
                            Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi (chưa lưu DB)"));
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        typingStatusLabel.setText("⚠️ P2P failed, trying server...");
                        sendViaServer(friendId, content);
                    });
                }
            });
        } else {
            // Fallback to server
            System.out.println("[ChatScene] 📤 Sending via Server");
            sendViaServer(friendId, content);
        }
    }
    
    private void sendViaServer(Long friendId, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setReceiverId(friendId);
                messageDTO.setContent(content);
                messageDTO.setMsgType("text");
                messageService.sendMessage(messageDTO);
                Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi qua Server"));
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Failed to send: " + e.getMessage());
                Platform.runLater(() -> typingStatusLabel.setText("❌ Gửi thất bại"));
            }
        });
    }
    
    private void scrollToBottom() {
        if (!messageListView.getItems().isEmpty()) {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        }
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
    private record ConversationItem(String name, String type, boolean online, String lastMessage, Long userId) {
        String getName() { return name; }
        String getType() { return type; }
        boolean isOnline() { return online; }
        String lastMessagePreview() { return lastMessage; }
        Long getUserId() { return userId; }
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
