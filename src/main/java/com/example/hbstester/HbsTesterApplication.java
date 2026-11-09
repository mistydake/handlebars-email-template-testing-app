package com.example.hbstester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class HbsTesterApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(HbsTesterApplication.class, args);

        // Auto-open browser after server starts
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Thread.sleep(1500); // Give server a moment to fully start
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        } else {
            System.out.println("Server started. Open your browser to: http://localhost:8080");
        }
    }
}
