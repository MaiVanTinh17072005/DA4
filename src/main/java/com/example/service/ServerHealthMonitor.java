package com.example.service;



import com.example.config.ApiConfig;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Monitors server health and notifies listeners of status changes
 */
public class ServerHealthMonitor {
    
    private static final int HEALTH_CHECK_INTERVAL_MS = 10000; // 10 seconds
    private static final int HEALTH_CHECK_TIMEOUT_MS = 2000; // 2 seconds
    
    private volatile boolean isServerOnline = false;
    private volatile boolean isMonitoring = false;
    
    private ScheduledExecutorService scheduler;
    private final List<ServerStatusListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface ServerStatusListener {
        void onServerOnline();
        void onServerOffline();
    }
    
    public ServerHealthMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ServerHealthMonitor");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start monitoring server health
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("[ServerHealthMonitor] Already monitoring");
            return;
        }
        
        isMonitoring = true;
        System.out.println("[ServerHealthMonitor] 🔍 Started monitoring server health");
        
        // Initial check
        checkServerHealth();
        
        // Schedule periodic checks
        scheduler.scheduleAtFixedRate(
            this::checkServerHealth,
            HEALTH_CHECK_INTERVAL_MS,
            HEALTH_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        isMonitoring = false;
        scheduler.shutdown();
        System.out.println("[ServerHealthMonitor] ⏹️ Stopped monitoring");
    }
    
    /**
     * Check if server is currently online
     */
    public boolean isServerOnline() {
        return isServerOnline;
    }
    
    /**
     * Add status listener
     */
    public void addListener(ServerStatusListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove status listener
     */
    public void removeListener(ServerStatusListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Perform health check
     */
    private void checkServerHealth() {
        boolean wasOnline = isServerOnline;
        boolean nowOnline = performHealthCheck();
        
        isServerOnline = nowOnline;
        
        // Notify listeners if status changed
        if (wasOnline != nowOnline) {
            if (nowOnline) {
                System.out.println("[ServerHealthMonitor] ✅ Server is ONLINE");
                notifyServerOnline();
            } else {
                System.out.println("[ServerHealthMonitor] ❌ Server is OFFLINE");
                notifyServerOffline();
            }
        }
    }
    
    /**
     * Perform actual HTTP health check
     */
    private boolean performHealthCheck() {
        try {
            String healthUrl = ApiConfig.BASE_URL + "/api/v1/health";
            URL url = new URL(healthUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
            conn.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } catch (Exception e) {
            // Any exception means server is offline
            return false;
        }
    }
    
    /**
     * Notify listeners that server is online
     */
    private void notifyServerOnline() {
        for (ServerStatusListener listener : listeners) {
            try {
                listener.onServerOnline();
            } catch (Exception e) {
                System.err.println("[ServerHealthMonitor] Error in listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify listeners that server is offline
     */
    private void notifyServerOffline() {
        for (ServerStatusListener listener : listeners) {
            try {
                listener.onServerOffline();
            } catch (Exception e) {
                System.err.println("[ServerHealthMonitor] Error in listener: " + e.getMessage());
            }
        }
    }
}
