module com.example.fe_app {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    // JSON
    requires com.fasterxml.jackson.databind;

    // Redis
    requires redis.clients.jedis;

    // Logging
    requires org.slf4j;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires com.google.gson;

    // Opens & Exports
    opens com.example.app to javafx.fxml, javafx.graphics;
    opens com.example.ui to javafx.fxml;
    opens com.example.service to javafx.fxml;
    opens com.example.api.dto to com.google.gson;

    exports com.example.app;
    exports com.example.ui;
    exports com.example.service;
    exports com.example.network;
    exports com.example.crypto;
    exports com.example.util;
}