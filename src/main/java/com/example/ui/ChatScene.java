package com.example.ui;

import com.example.api.dto.MessageDTO;
import com.example.api.dto.UserDTO;
import com.example.network.P2PManager;
import com.example.network.P2PMessageListener;
import com.example.network.PeerManager;
import com.example.network.model.P2PMessage;
import com.example.service.*;
import com.example.util.SceneManager;
import com.example.util.SessionManager;
import com.example.crypto.*;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    
    // Group-related buttons (only visible in Group filter)
    @FXML private Button createGroupButton;
    @FXML private Button addMemberButton;
    @FXML private Label groupSectionHeader;

    private final ObservableList<ConversationItem> allConversations = FXCollections.observableArrayList();
    private final ObservableList<ConversationItem> filteredConversations = FXCollections.observableArrayList();
    private final ObservableList<com.example.api.dto.GroupDTO> allGroups = FXCollections.observableArrayList();
    private ConversationItem activeConversation;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    // Services
    private final FriendService friendService = new FriendService();
    private final MessageService messageService = new MessageService();
    private final com.example.service.GroupService groupService = new com.example.service.GroupService();
    
    // P2P Manager
    private P2PManager p2pManager;
    private boolean p2pEnabled = false;
    
    // Hybrid P2P Services
    private ServerHealthMonitor serverHealthMonitor;
    private MessageSyncService messageSyncService;
    private LocalRedisService localRedis;  // ✅ Local Redis instead of file-based queue
    private final RedisQueueService redisQueueService = new RedisQueueService();

    // E2EE States
    private SecretKey masterKey;
    private KeyPair identityKeyPair;
    private final Map<Long, SecretKey> conversationKeys = new HashMap<>();

    @FXML
    public void initialize() {
        initProfile();
        initE2EE(); // NEW: Initialize E2EE
        initP2P();
        initHybridServices();
        initConversations();
        initConversationList();
        initMessageView();
        initOnlineList();
        initGroupOverview();
        initFilters();
        selectDefaultConversation();
        typingStatusLabel.setText("Sẵn sàng chat. Tin nhắn sẽ tự động gửi lại khi server online.");
    }

    private void initE2EE() {
        try {
            String password = SessionManager.getUserPassword();
            String saltStr = SessionManager.getUserSalt();
            
            if (password == null || saltStr == null) {
                System.err.println("[ChatScene] E2EE initialization failed: Missing password or salt");
                encryptionStatusLabel.setText("🔓 E2EE không khả dụng (thiếu key)");
                return;
            }
            
            byte[] salt = Base64.getDecoder().decode(saltStr);
            
            // Derive master key
            masterKey = E2EEManager.deriveMasterKey(password, salt);
            System.out.println("[ChatScene] Master key derived");
            
            // Load or generate local identity
            PrivateKey privateKey = KeyStore.loadPrivateKey(masterKey);
            if (privateKey != null) {
                System.out.println("[ChatScene] Local identity loaded");
                // Need to regenerate public key from private or store it separately
                // For ECDH, we can re-derive public from private or just store the full pair
                // For simplicity, let's generate if missing or store properly
                // Here we'll generate new one if load fails, and upload to server
                identityKeyPair = new KeyPair(null, privateKey); // Public key should be fetched from server if missing
            } else {
                System.out.println("[ChatScene] No local identity, generating new one...");
                identityKeyPair = E2EEManager.generateECDHKeyPair();
                KeyStore.savePrivateKey(identityKeyPair.getPrivate(), masterKey);
                
                // Upload public key to server
                String pubKeyStr = E2EEManager.publicKeyToString(identityKeyPair.getPublic());
                UserKeyService.uploadPublicKey(pubKeyStr);
                System.out.println("[ChatScene] New identity generated and uploaded");
            }
            
            encryptionStatusLabel.setText("🔒 E2EE đang hoạt động");
        } catch (Exception e) {
            System.err.println("[ChatScene] E2EE initialization error: " + e.getMessage());
            e.printStackTrace();
            encryptionStatusLabel.setText("🔓 E2EE gặp lỗi");
        }
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

    private void loadMessagesFor(ConversationItem conversation) {        activeConversationLabel.setText("# " + conversation.getName());
        activeStatusLabel.setText(conversation.isOnline()
            ? "🟢 Đang hoạt động • P2P sẵn sàng"
            : "⚫ Ngoại tuyến • Tin nhắn sẽ được lưu hàng đợi");

        // Check if this is a group conversation
        if ("Group".equals(conversation.getType())) {
            // Update header for group
            activeConversationLabel.setText("👥 " + conversation.getName());
            
            // Get group details to show member count
            Long groupId = conversation.getUserId();
            CompletableFuture.runAsync(() -> {
                try {
                    com.example.api.dto.GroupDTO groupDetails = groupService.getGroupDetails(groupId);
                    Platform.runLater(() -> {
                        activeStatusLabel.setText("👥 " + groupDetails.getMemberCount() + " thành viên");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        activeStatusLabel.setText("👥 Nhóm");
                    });
                }
            });
            
            // Show add member button for groups
            if (addMemberButton != null) {
                addMemberButton.setVisible(true);
                addMemberButton.setManaged(true);
            }
            loadGroupMessages(conversation);
            return;
        } else {
            // Hide add member button for DMs
            if (addMemberButton != null) {
                addMemberButton.setVisible(false);
                addMemberButton.setManaged(false);
            }
        }
        
        // Load real messages from API (Direct messages)
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
                    
                    // Decrypt if message is encrypted
                    String displayContent = msg.getContent();
                    if (msg.getAesEncrypted() != null && msg.getAesEncrypted()) {
                        try {
                            SecretKey convKey = getConversationKey(friendId);
                            if (convKey != null) {
                                EncryptedMessage encrypted = new EncryptedMessage(
                                    Base64.getDecoder().decode(msg.getContent()),
                                    Base64.getDecoder().decode(msg.getIv()),
                                    Base64.getDecoder().decode(msg.getAuthTag()),
                                    msg.getAlgorithm()
                                );
                                displayContent = E2EEManager.decryptMessage(encrypted, convKey);
                            } else {
                                displayContent = "[Tin nhắn mã hóa - Thiếu Key]";
                            }
                        } catch (Exception e) {
                            System.err.println("[ChatScene] Decryption failed: " + e.getMessage());
                            displayContent = "[Lỗi giải mã tin nhắn]";
                        }
                    }
                    
                    messages.add(new MessageItem(author, displayContent, timestamp, isOwn));
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
    
    private void initHybridServices() {
        try {
            // Initialize server health monitor
            serverHealthMonitor = new ServerHealthMonitor();
            serverHealthMonitor.startMonitoring();
            
            // Initialize local message queue
            localRedis = LocalRedisService.getInstance();
            
            // Initialize message sync service
            messageSyncService = new MessageSyncService(
                serverHealthMonitor,
                localRedis,
                messageService
            );
            
            // Set sync status listener for UI updates
            messageSyncService.setSyncStatusListener(new MessageSyncService.SyncStatusListener() {
                @Override
                public void onSyncStarted(int messageCount) {
                    Platform.runLater(() -> {
                        typingStatusLabel.setText("🔄 Đang đồng bộ " + messageCount + " tin nhắn...");
                    });
                }
                
                @Override
                public void onSyncProgress(int sent, int total) {
                    Platform.runLater(() -> {
                        typingStatusLabel.setText("🔄 Đã đồng bộ " + sent + "/" + total);
                    });
                }
                
                @Override
                public void onSyncCompleted(int successful, int failed) {
                    Platform.runLater(() -> {
                        if (failed == 0) {
                            typingStatusLabel.setText("✅ Đồng bộ hoàn tất: " + successful + " tin nhắn");
                        } else {
                            typingStatusLabel.setText("⚠️ Đồng bộ: " + successful + " thành công, " + failed + " thất bại");
                        }
                    });
                }
            });
            
            messageSyncService.start();
            
            System.out.println("[ChatScene] ✅ Hybrid P2P services initialized");
            
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Failed to initialize hybrid services: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("[ChatScene] 📨 P2P message received from: " + message.getSenderId());
        System.out.println("  - Encrypted: " + message.isEncrypted());
        
        Platform.runLater(() -> {
            // Check if message is from active conversation
            if (activeConversation != null && message.getSenderId().equals(activeConversation.getUserId())) {
                
                String displayContent = message.getContent();  // Default
                
                // ===== TRUE E2EE: GIẢI MÃ KHI NHẬN TỪ PEER =====
                if (message.isEncrypted() && message.getIv() != null && message.getAuthTag() != null) {
                    try {
                        SecretKey convKey = getConversationKey(message.getSenderId());
                        if (convKey != null) {
                            // Giải mã message
                            byte[] ciphertext = Base64.getDecoder().decode(message.getContent());
                            byte[] ivBytes = Base64.getDecoder().decode(message.getIv());
                            byte[] authTagBytes = Base64.getDecoder().decode(message.getAuthTag());
                            
                            EncryptedMessage encrypted = new EncryptedMessage(
                                ciphertext,
                                ivBytes,
                                authTagBytes,
                                message.getAlgorithm()
                            );
                            
                            displayContent = E2EEManager.decryptMessage(encrypted, convKey);
                            System.out.println("[ChatScene] 🔓 Message decrypted successfully (TRUE E2EE)");
                            System.out.println("  - Plain text: " + displayContent);
                        } else {
                            System.err.println("[ChatScene] ❌ No conversation key for decryption");
                            displayContent = "[Không thể giải mã - thiếu key]";
                        }
                    } catch (Exception e) {
                        System.err.println("[ChatScene] ❌ Decryption failed: " + e.getMessage());
                        e.printStackTrace();
                        displayContent = "[Lỗi giải mã]";
                    }
                } else if (message.isEncrypted()) {
                    System.out.println("[ChatScene] ⚠️ Encrypted message but missing IV/AuthTag");
                    displayContent = "[Encrypted - thiếu thông tin giải mã]";
                }
                
                // Add message to UI
                LocalDateTime timestamp = LocalDateTime.ofEpochSecond(
                    message.getTimestamp() / 1000, 
                    0, 
                    java.time.ZoneOffset.UTC
                );
                
                MessageItem item = new MessageItem(
                    message.getSenderUsername() != null ? message.getSenderUsername() : "Friend",
                    displayContent,  // ✅ Plain text (đã giải mã)
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
       
        // Check if this is a group conversation
        if ("Group".equals(activeConversation.getType())) {
            handleSendGroupMessage(text.trim());
            return;
        }
        
        // Direct message handling
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
        
        
        // ===== TRUE E2EE: MÃ HÓA TRƯỚC KHI GỬI =====
        String finalContent = content;  // Plain text ban đầu
        String iv = null;
        String authTag = null;
        String algorithm = "AES-256-GCM";
        boolean isEncrypted = false;
        
        try {
            SecretKey convKey = getConversationKey(friendId);
            if (convKey != null) {
                // MÃ HÓA NGAY - một lần duy nhất cho TẤT CẢ
                EncryptedMessage encrypted = E2EEManager.encryptMessage(content, convKey);
                
                // Chuyển sang Base64 để truyền qua network
                finalContent = Base64.getEncoder().encodeToString(encrypted.getCiphertext());
                iv = Base64.getEncoder().encodeToString(encrypted.getIv());
                authTag = Base64.getEncoder().encodeToString(encrypted.getAuthTag());
                algorithm = encrypted.getAlgorithm();
                isEncrypted = true;
                
                System.out.println("[ChatScene] 🔒 TRUE E2EE - Message encrypted:");
                System.out.println("  - Plain: " + content);
                System.out.println("  - Encrypted: " + finalContent.substring(0, Math.min(20, finalContent.length())) + "...");
                System.out.println("  - Will use for P2P + DB + Redis ✅");
            } else {
                System.out.println("[ChatScene] ⚠️ WARNING: No conversation key - sending plain text");
            }
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Encryption failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Create MessageDTO với encrypted content (hoặc plain nếu không có key)
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setMsgId(System.currentTimeMillis());
        messageDTO.setSenderId(currentUser.getId());
        messageDTO.setSenderUsername(currentUser.getUsername());
        messageDTO.setReceiverId(friendId);
        messageDTO.setContent(finalContent);  // ✅ ENCRYPTED content
        messageDTO.setMsgType("text");
        messageDTO.setTimestamp(LocalDateTime.now().toString());
        messageDTO.setAesEncrypted(isEncrypted);
        
        if (isEncrypted) {
            messageDTO.setIv(iv);
            messageDTO.setAuthTag(authTag);
            messageDTO.setAlgorithm(algorithm);
        }
        
        System.out.println("[ChatScene] 📝 Created MessageDTO:");
        System.out.println("  - SenderId: " + messageDTO.getSenderId());
        System.out.println("  - ReceiverId: " + messageDTO.getReceiverId());
        System.out.println("  - Encrypted: " + messageDTO.getAesEncrypted());
        
        // HYBRID P2P LOGIC
        boolean hasP2P = p2pEnabled && p2pManager != null && p2pManager.isConnectedToFriend(friendId);
        boolean serverOnline = serverHealthMonitor != null && serverHealthMonitor.isServerOnline();
        
        if (hasP2P) {
            // Priority 1: Send via P2P với ENCRYPTED content + E2EE fields
            System.out.println("[ChatScene] 📤 Sending ENCRYPTED message via P2P (TRUE E2EE)");
            
            // ✅ TẠO P2PMessage VỚI E2EE FIELDS
            P2PMessage p2pMessage = P2PMessage.createTextMessage(currentUser.getId(), friendId, finalContent);
            p2pMessage.setSenderUsername(currentUser.getUsername());
            p2pMessage.setEncrypted(isEncrypted);
            
            if (isEncrypted) {
                p2pMessage.setIv(iv);
                p2pMessage.setAuthTag(authTag);
                p2pMessage.setAlgorithm(algorithm);
                System.out.println("[ChatScene] 🔐 P2PMessage with E2EE fields:");
                System.out.println("  - Encrypted: true");
                System.out.println("  - IV: " + iv.substring(0, Math.min(10, iv.length())) + "...");
                System.out.println("  - AuthTag: " + authTag.substring(0, Math.min(10, authTag.length())) + "...");
            }
            
            // Gửi P2PMessage (cần sửa P2PManager để nhận P2PMessage thay vì String)
            // Tạm thời dùng sendTextMessage, sau đó sẽ tạo method mới
            p2pManager.sendTextMessage(friendId, finalContent).thenAccept(p2pSuccess -> {
                if (p2pSuccess) {
                    System.out.println("[ChatScene] ✅ Encrypted P2P message sent (TRUE E2EE)");
                    
                    // ✅ FIX: Always queue to local Redis regardless of server status
                    // The MessageSyncService will handle syncing to server when online
                    localRedis.queueMessage(messageDTO);
                    
                    if (serverOnline) {
                        Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi P2P (⏳ sync queue)"));
                        System.out.println("[ChatScene] 📝 Message queued to local Redis for sync");
                    } else {
                        Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi P2P (⏳ local queue)"));
                        System.out.println("[ChatScene] 📝 Message queued to local Redis (server offline)");
                    }
                } else {
                    // P2P failed, try server
                    Platform.runLater(() -> typingStatusLabel.setText("⚠️ P2P thất bại, thử server..."));
                    // ✅ FIX: Queue to local Redis first (only once)
                    localRedis.queueMessage(messageDTO);
                    sendViaServer(messageDTO);
                }
            });
        } else {
            // No P2P connection, must use server
            // ✅ FIX: Queue to local Redis first (only once)
            localRedis.queueMessage(messageDTO);
            sendViaServer(messageDTO);
        }
    }
    
    
    private SecretKey getConversationKey(Long friendId) {
        if (conversationKeys.containsKey(friendId)) {
            return conversationKeys.get(friendId);
        }
        
        try {
            // 1. Load friend's public key
            PublicKey friendPubKey = KeyStore.loadFriendPublicKey(friendId);
            if (friendPubKey == null) {
                // Fetch from server
                String pubKeyStr = UserKeyService.fetchFriendPublicKey(friendId);
                if (pubKeyStr != null) {
                    friendPubKey = E2EEManager.stringToPublicKey(pubKeyStr);
                    KeyStore.saveFriendPublicKey(friendId, pubKeyStr);
                }
            }
            
            if (friendPubKey == null) {
                System.err.println("[ChatScene] Could not get public key for friend: " + friendId);
                return null;
            }
            
            // 2. Get my private key
            PrivateKey myPrivKey = identityKeyPair.getPrivate();
            if (myPrivKey == null) {
                // Should not happen if initE2EE was successful
                return null;
            }
            
            // 3. Derive shared secret
            SecretKey sharedKey = E2EEManager.deriveConversationKey(myPrivKey, friendPubKey);
            conversationKeys.put(friendId, sharedKey);
            return sharedKey;
        } catch (Exception e) {
            System.err.println("[ChatScene] Error deriving conversation key: " + e.getMessage());
            return null;
        }
    }
    private void sendViaServer(MessageDTO messageDTO) {
        if (serverHealthMonitor != null && !serverHealthMonitor.isServerOnline()) {
            // ✅ FIX: Server offline - message already queued by caller
            // Do NOT queue again to avoid duplicates
            System.out.println("[ChatScene] ⚠️ Server offline - message already in local queue");
            Platform.runLater(() -> typingStatusLabel.setText("⏳ Server offline - local queue"));
            return;
        }
        
        sendToServerAsync(messageDTO, true);
    }
    
    private void sendToServerAsync(MessageDTO messageDTO, boolean showStatus) {
        System.out.println("[ChatScene] 📤 sendToServerAsync called");
        System.out.println("  - ShowStatus: " + showStatus);
        System.out.println("  - MessageDTO: SenderId=" + messageDTO.getSenderId() + ", ReceiverId=" + messageDTO.getReceiverId());
        
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[ChatScene] 🚀 Calling messageService.sendMessage()...");
                MessageDTO result = messageService.sendMessage(messageDTO);
                if (result != null) {
                    System.out.println("[ChatScene] ✅ Message sent to server successfully!");
                    System.out.println("  - Returned MsgId: " + result.getMsgId());
                    if (showStatus) {
                        Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi qua Server"));
                    }
                } else {
                    throw new Exception("Server returned null");
                }
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Failed to send to server: " + e.getMessage());
                e.printStackTrace();
                
                // ✅ FIX: Do NOT re-queue - message is already in Local Redis from initial send
                // Re-queuing here causes duplicates when the message is synced later
                System.err.println("[ChatScene] ⚠️ Message send failed - already in queue for retry");
                
                if (showStatus) {
                    Platform.runLater(() -> typingStatusLabel.setText("⏳ Lỗi gửi - đã trong queue"));
                }
            }
        });
    }
    
    
    private void scrollToBottom() {
        if (!messageListView.getItems().isEmpty()) {
            messageListView.scrollTo(messageListView.getItems().size() - 1);
        }
    }
    
    // ===== Group Chat Methods =====
    
    /**
     * Load groups from local Redis cache (loaded on login)
     * Falls back to API if cache is empty
     */
    private void loadGroups() {
        System.out.println("[ChatScene] Loading groups from local Redis...");
        
        CompletableFuture.runAsync(() -> {
            try {
                Long userId = SessionManager.getCurrentUserId();
                if (userId == null) {
                    System.err.println("[ChatScene] Cannot load groups - user ID is null");
                    return;
                }
                
                // Try to load from local Redis first
                List<com.example.api.dto.GroupDTO> groups = localRedis.getGroupsList(userId);
                
                // If cache is empty, fetch from API
                if (groups == null || groups.isEmpty()) {
                    System.out.println("[ChatScene] Cache empty, fetching from API...");
                    groups = groupService.getMyGroups();
                    
                    // Update cache
                    if (groups != null && !groups.isEmpty()) {
                        localRedis.saveGroupsList(userId, groups);
                        System.out.println("[ChatScene] ✅ Updated cache with " + groups.size() + " groups");
                    }
                } else {
                    System.out.println("[ChatScene] ✅ Loaded " + groups.size() + " groups from cache");
                }
                
                final List<com.example.api.dto.GroupDTO> finalGroups = groups;
                
                Platform.runLater(() -> {
                    if (finalGroups != null) {
                        allGroups.setAll(finalGroups);
                        
                        // Remove old group conversations
                        allConversations.removeIf(conv -> "Group".equals(conv.getType()));
                        
                        // Add groups to conversation list
                        for (com.example.api.dto.GroupDTO group : finalGroups) {
                            ConversationItem item = new ConversationItem(
                                "👥 " + group.getName(),  // Add group icon
                                "Group",
                                true, // Groups are always "online"
                                group.getMemberCount() + " thành viên",  // Show member count
                                group.getGroupId()
                            );
                            allConversations.add(item);
                        }
                        
                        System.out.println("[ChatScene] ✅ Displayed " + finalGroups.size() + " group(s)");
                        applyFilters();
                    }
                });
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Error loading groups: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    typingStatusLabel.setText("⚠ Không thể tải danh sách nhóm");
                });
            }
        });
    }
    
    /**
     * Create new group dialog
     */
    @FXML
    private void handleCreateGroup() {
        try {
            // Load custom dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/dialog/create-group-dialog.fxml"));
            Parent dialogRoot = loader.load();
            
            // Get controller
            CreateGroupDialog dialogController = loader.getController();
            
            // Load CSS
            dialogRoot.getStylesheets().add(getClass().getResource("/com/example/css/create-group-dialog.css").toExternalForm());
            
            // Create stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Tạo nhóm mới");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(messageInput.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            
            // Show and wait
            dialogStage.showAndWait();
            
            // Check if confirmed
            if (dialogController.isConfirmed()) {
                String groupName = dialogController.getGroupName();
                String description = dialogController.getDescription();
                List<Long> memberIds = dialogController.getMemberIds();
                
                System.out.println("[ChatScene] Creating group: " + groupName + " with " + memberIds.size() + " members");
                
                // Create group via API
                createGroupAsync(groupName, description, memberIds);
            }
            
        } catch (Exception e) {
            System.err.println("[ChatScene] Error showing create group dialog: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể mở dialog tạo nhóm");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Show dialog to select members from friends list
     */
    private List<Long> selectMembersDialog() {
        List<Long> selectedMembers = new ArrayList<>();
        
        try {
            List<UserDTO> friends = friendService.getFriendsList();
            
            if (friends == null || friends.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Bạn chưa có bạn bè nào để thêm vào nhóm.");
                alert.showAndWait();
                return selectedMembers;
            }
            
            // Simple selection using ChoiceDialog (for now)
            // TODO: Create custom multi-select dialog
            ChoiceDialog<UserDTO> dialog = new ChoiceDialog<>(friends.get(0), friends);
            dialog.setTitle("Chọn thành viên");
            dialog.setHeaderText("Chọn bạn bè để thêm vào nhóm");
            dialog.setContentText("Chọn người dùng:");
            
            dialog.showAndWait().ifPresent(friend -> {
                selectedMembers.add(friend.getId());
            });
            
        } catch (Exception e) {
            System.err.println("[ChatScene] Error selecting members: " + e.getMessage());
        }
        
        return selectedMembers;
    }
    
    /**
     * Create group via API asynchronously
     */
    private void createGroupAsync(String name, String description, List<Long> memberIds) {
        CompletableFuture.runAsync(() -> {
            try {
                com.example.api.dto.GroupDTO createdGroup = groupService.createGroup(name, description, memberIds);
                
                Platform.runLater(() -> {
                    System.out.println("[ChatScene] ✅ Group created: " + createdGroup.getName());
                    typingStatusLabel.setText("✅ Nhóm '" + createdGroup.getName() + "' đã được tạo");
                    
                    // Reload groups
                    loadGroups();
                });
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Failed to create group: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi");
                    alert.setHeaderText("Không thể tạo nhóm");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }
    
    /**
     * Handle adding member to active group
     */
    @FXML
    private void handleAddMember() {
        if (activeConversation == null || !"Group".equals(activeConversation.getType())) {
            typingStatusLabel.setText("⚠ Vui lòng chọn một nhóm trước");
            return;
        }
        
        Long groupId = activeConversation.getUserId();
        List<Long> memberIds = selectMembersDialog();
        
        if (!memberIds.isEmpty()) {
            addMemberAsync(groupId, memberIds.get(0));
        }
    }
    
    /**
     * Add member to group via API
     */
    private void addMemberAsync(Long groupId, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean success = groupService.addMember(groupId, userId);
                
                Platform.runLater(() -> {
                    if (success) {
                        typingStatusLabel.setText("✅ Đã thêm thành viên vào nhóm");
                    } else {
                        typingStatusLabel.setText("⚠ Không thể thêm thành viên");
                    }
                });
            } catch (Exception e) {
                System.err.println("[ChatScene] ❌ Failed to add member: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi");
                    alert.setHeaderText("Không thể thêm thành viên");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }
    
    /**
     * Load group messages from API
     */
    private void loadGroupMessages(ConversationItem conversation) {
        System.out.println("[ChatScene] Loading group messages for: " + conversation.getName());
        
        try {
            Long groupId = conversation.getUserId();
            List<MessageDTO> apiMessages = messageService.getGroupMessages(groupId);
            
            ObservableList<MessageItem> messages = FXCollections.observableArrayList();
            
            if (apiMessages != null && !apiMessages.isEmpty()) {
                System.out.println("[ChatScene] ✅ Loaded " + apiMessages.size() + " group message(s)");
                
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
                    
                    // Group messages are typically not encrypted for now
                    String displayContent = msg.getContent();
                    
                    messages.add(new MessageItem(author, displayContent, timestamp, isOwn));
                }
            } else {
                System.out.println("[ChatScene] ℹ️ No group messages found");
            }
            
            messageListView.setItems(messages);
            if (!messages.isEmpty()) {
                messageListView.scrollTo(messages.size() - 1);
            }
            
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Error loading group messages: " + e.getMessage());
            e.printStackTrace();
            messageListView.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Handle sending group message
     */
    private void handleSendGroupMessage(String content) {
        UserDTO currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        
        Long groupId = activeConversation.getUserId();
        
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
        
        // Create MessageDTO for group message
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setMsgId(System.currentTimeMillis());
        messageDTO.setSenderId(currentUser.getId());
        messageDTO.setSenderUsername(currentUser.getUsername());
        messageDTO.setGroupId(groupId);  // Set groupId for group messages
        messageDTO.setContent(content);
        messageDTO.setMsgType("text");
        messageDTO.setTimestamp(LocalDateTime.now().toString());
        messageDTO.setAesEncrypted(false); // Group messages not encrypted for now
        
        System.out.println("[ChatScene] 📝 Sending group message to groupId: " + groupId);
        
        // Try to send via server
        boolean serverOnline = serverHealthMonitor != null && serverHealthMonitor.isServerOnline();
        
        if (serverOnline) {
            // Send via server
            sendToServerAsync(messageDTO, true);
        } else {
            // Queue locally
            localRedis.queueMessage(messageDTO);
            typingStatusLabel.setText("⏳ Server offline - message queued");
        }
        
        // TODO: Implement P2P multicast to all online group members
        // This requires getting list of group members and sending to each via P2P
    }
    
    /**
     * Cleanup when scene is closed
     */
    public void cleanup() {
        if (serverHealthMonitor != null) {
            serverHealthMonitor.stopMonitoring();
        }
        if (messageSyncService != null) {
            messageSyncService.stop();
        }
        if (localRedis != null) {
            // Redis auto-persists, no need to manually save
        }
        System.out.println("[ChatScene] 🧹 Cleanup completed");
    }

    @FXML
    private void handleInsertEmoji() {
        messageInput.appendText(" 😀 ");
        typingStatusLabel.setText("Đã thêm emoji.");
    }

    // ===== Filter Buttons =====
    @FXML 
    private void handleFilterDM() {
        // Clear active conversation when switching to DM
        activeConversation = null;
        messageListView.setItems(FXCollections.observableArrayList());
        activeConversationLabel.setText("# Chọn một cuộc trò chuyện");
        activeStatusLabel.setText("Chọn bạn bè để bắt đầu nhắn tin");
        
        // Hide group-related buttons when in DM mode
        if (createGroupButton != null) {
            createGroupButton.setVisible(false);
            createGroupButton.setManaged(false);
        }
        if (addMemberButton != null) {
            addMemberButton.setVisible(false);
            addMemberButton.setManaged(false);
        }
        if (groupSectionHeader != null) {
            groupSectionHeader.setVisible(false);
            groupSectionHeader.setManaged(false);
        }
        applyFilters(); 
    }
    
    @FXML 
    private void handleFilterGroup() {
        // Clear active conversation when switching to Group
        activeConversation = null;
        messageListView.setItems(FXCollections.observableArrayList());
        activeConversationLabel.setText("# Chọn một nhóm");
        activeStatusLabel.setText("Chọn nhóm để xem tin nhắn");
        
        // Show group-related buttons when in Group mode
        if (createGroupButton != null) {
            createGroupButton.setVisible(true);
            createGroupButton.setManaged(true);
        }
        if (groupSectionHeader != null) {
            groupSectionHeader.setVisible(true);
            groupSectionHeader.setManaged(true);
        }
        // Add member button will be shown/hidden based on active conversation
        
        // Load groups when filter is selected
        loadGroups();
        applyFilters();
    }
    
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
