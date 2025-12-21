package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot Application
 * Entry point for Discord Mini Server
 */
@SpringBootApplication
@ComponentScan(basePackages = {"app", "controller", "service", "repository", "util", "dto", "model", "config", "scheduler"})
@EntityScan(basePackages = "model")
@EnableJpaRepositories(basePackages = "repository")
public class ServerMain {
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("Discord Mini Server Starting...");
        System.out.println("=================================");
        
        SpringApplication.run(ServerMain.class, args);
        
        System.out.println("=================================");
        System.out.println("Server Started Successfully!");

        System.out.println("=================================");
    }
}
