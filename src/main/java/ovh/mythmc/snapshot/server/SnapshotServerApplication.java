package ovh.mythmc.snapshot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Snapshot Server.
 * A Spring Boot application that provides a service for collecting and viewing
 * Minecraft server debug information.
 * 
 * @author U8092
 * @version 0.1.0
 */
@SpringBootApplication
@EnableScheduling
public class SnapshotServerApplication {

    /**
     * Main entry point for the application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SnapshotServerApplication.class, args);
    }
}