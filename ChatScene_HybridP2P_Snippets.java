// ============================================================
// CHATSCENE.JAVA - CODE SNIPPETS FOR HYBRID P2P INTEGRATION
// ============================================================

// ===== STEP 1: ADD IMPORTS (after line 30) =====
import com.example.exception.ServerOfflineException;
import com.example.service.ServerHealthMonitor;
import com.example.service.LocalMessageQueue;
import com.example.service.MessageSyncService;


// ===== STEP 2: ADD SERVICE FIELDS (after line 69) =====
// Hybrid P2P Services
private ServerHealthMonitor serverHealthMonitor;
private LocalMessageQueue localMessageQueue;
private MessageSyncService messageSyncService;


// ===== STEP 3: REPLACE initialize() METHOD (lines 71-83) =====
@FXML
public void initialize() {
    initProfile();
    initP2P();
    initHybridServices();  // NEW: Initialize hybrid P2P services
    initConversations();
    initConversationList();
    initMessageView();
    initOnlineList();
    initGroupOverview();
    initFilters();
    selectDefaultConversation();
    typingStatusLabel.setText("Sẵn sàng chat. Tin nhắn sẽ tự động gửi lại khi server online.");
}


// ===== STEP 4: ADD initHybridServices() METHOD (after initP2P(), after line 427) =====
private void initHybridServices() {
    try {
        // Initialize server health monitor
        serverHealthMonitor = new ServerHealthMonitor();
        serverHealthMonitor.startMonitoring();
        
        // Initialize local message queue
        localMessageQueue = new LocalMessageQueue();
        
        // Initialize message sync service
        messageSyncService = new MessageSyncService(
            serverHealthMonitor,
            localMessageQueue,
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


// ===== STEP 5: REPLACE handleSendMessage() METHOD (lines 656-722) =====
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
    
    // Create message DTO
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setMessageId(java.util.UUID.randomUUID().toString());
    messageDTO.setReceiverId(friendId);
    messageDTO.setContent(content);
    messageDTO.setMsgType("text");
    
    // HYBRID P2P LOGIC
    boolean hasP2P = p2pEnabled && p2pManager != null && p2pManager.isConnectedToFriend(friendId);
    boolean serverOnline = serverHealthMonitor != null && serverHealthMonitor.isServerOnline();
    
    if (hasP2P) {
        // Priority 1: Send via P2P
        System.out.println("[ChatScene] 📤 Sending via P2P");
        
        p2pManager.sendTextMessage(friendId, content).thenAccept(p2pSuccess -> {
            if (p2pSuccess) {
                System.out.println("[ChatScene] ✅ P2P message sent");
                Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi qua P2P (E2EE)"));
                
                // If server online, persist to database
                if (serverOnline) {
                    sendToServerAsync(messageDTO, false);
                } else {
                    // Server offline, add to local queue
                    localMessageQueue.addPendingMessage(messageDTO);
                    Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi P2P (⏳ chờ đồng bộ)"));
                }
            } else {
                // P2P failed, try server
                Platform.runLater(() -> typingStatusLabel.setText("⚠️ P2P thất bại, thử server..."));
                sendViaServer(messageDTO);
            }
        });
    } else {
        // No P2P connection, must use server
        sendViaServer(messageDTO);
    }
}


// ===== STEP 6: REPLACE sendViaServer() METHOD (lines 724-738) =====
private void sendViaServer(MessageDTO messageDTO) {
    if (serverHealthMonitor != null && !serverHealthMonitor.isServerOnline()) {
        // Server offline, add to local queue
        System.out.println("[ChatScene] ⚠️ Server offline, adding to local queue");
        localMessageQueue.addPendingMessage(messageDTO);
        Platform.runLater(() -> typingStatusLabel.setText("⏳ Server offline - tin nhắn sẽ gửi lại tự động"));
        return;
    }
    
    sendToServerAsync(messageDTO, true);
}

private void sendToServerAsync(MessageDTO messageDTO, boolean showStatus) {
    CompletableFuture.runAsync(() -> {
        try {
            MessageDTO result = messageService.sendMessage(messageDTO);
            if (result != null) {
                System.out.println("[ChatScene] ✅ Message sent to server");
                if (showStatus) {
                    Platform.runLater(() -> typingStatusLabel.setText("✓ Đã gửi qua Server"));
                }
            } else {
                throw new Exception("Server returned null");
            }
        } catch (Exception e) {
            System.err.println("[ChatScene] ❌ Failed to send to server: " + e.getMessage());
            
            // Add to local queue for retry
            localMessageQueue.addPendingMessage(messageDTO);
            
            if (showStatus) {
                Platform.runLater(() -> typingStatusLabel.setText("⏳ Lỗi gửi - sẽ thử lại tự động"));
            }
        }
    });
}


// ===== STEP 7: ADD cleanup() METHOD (before helper classes, around line 755) =====
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
    if (localMessageQueue != null) {
        localMessageQueue.saveToDisk();
    }
    System.out.println("[ChatScene] 🧹 Cleanup completed");
}
