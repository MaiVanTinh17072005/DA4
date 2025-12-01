package com.example.network;

import com.example.config.P2PConfig;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * TCP Server
 * Server TCP để nhận kết nối từ các peers
 */
public class TCPServer {
    
    private static final Logger LOGGER = Logger.getLogger(TCPServer.class.getName());
    
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread serverThread;
    private final TCPMessageHandler messageHandler;
    
    public TCPServer(int port, TCPMessageHandler messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }
    
    /**
     * Khởi động TCP server
     */
    public void start() {
        if (running.get()) {
            LOGGER.warning("TCP Server đã đang chạy trên port " + port);
            return;
        }
        
        serverThread = Thread.ofVirtual()
                .name(P2PConfig.THREAD_NAME_PREFIX_TCP + "Server-" + port)
                .start(() -> {
                    try {
                        serverSocket = new ServerSocket(port, P2PConfig.TCP_BACKLOG);
                        serverSocket.setReuseAddress(true);
                        running.set(true);
                        
                        LOGGER.info("TCP Server đang lắng nghe trên port " + port);
                        
                        while (running.get() && !Thread.currentThread().isInterrupted()) {
                            try {
                                Socket clientSocket = serverSocket.accept();
                                handleClient(clientSocket);
                            } catch (SocketException e) {
                                if (running.get()) {
                                    LOGGER.warning("Socket exception: " + e.getMessage());
                                }
                            } catch (IOException e) {
                                LOGGER.severe("Lỗi khi accept connection: " + e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.severe("Không thể khởi động TCP Server: " + e.getMessage());
                    } finally {
                        cleanup();
                    }
                });
    }
    
    /**
     * Xử lý client connection trong Virtual Thread riêng
     */
    private void handleClient(Socket clientSocket) {
        Thread.ofVirtual()
                .name(P2PConfig.THREAD_NAME_PREFIX_TCP + "Client-" + clientSocket.getRemoteSocketAddress())
                .start(() -> {
                    try {
                        clientSocket.setSoTimeout(P2PConfig.TCP_SO_TIMEOUT);
                        
                        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                        
                        LOGGER.info("Peer đã kết nối: " + clientSocket.getRemoteSocketAddress());
                        
                        // Đọc messages từ peer
                        while (running.get() && !clientSocket.isClosed()) {
                            try {
                                Object message = in.readObject();
                                messageHandler.handleMessage(message, out);
                            } catch (EOFException e) {
                                LOGGER.info("Peer đã ngắt kết nối: " + clientSocket.getRemoteSocketAddress());
                                break;
                            } catch (ClassNotFoundException e) {
                                LOGGER.warning("Nhận được object không xác định: " + e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.warning("Lỗi khi xử lý client: " + e.getMessage());
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            LOGGER.warning("Lỗi khi đóng client socket: " + e.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Dừng TCP server
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        LOGGER.info("Đang dừng TCP Server trên port " + port);
        running.set(false);
        
        cleanup();
        
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
    
    /**
     * Dọn dẹp resources
     */
    private void cleanup() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                LOGGER.info("TCP Server đã đóng port " + port);
            } catch (IOException e) {
                LOGGER.warning("Lỗi khi đóng server socket: " + e.getMessage());
            }
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public int getPort() {
        return port;
    }
}
