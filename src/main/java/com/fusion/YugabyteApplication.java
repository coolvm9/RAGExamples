package com.fusion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.fusion")
public class YugabyteApplication {

    // Initialize the logger for this class
    private static final Logger logger = LoggerFactory.getLogger(YugabyteApplication.class);

    public static void main(String[] args) {
        // Log before starting the application
        logger.info("Starting YugabyteApplication...");

        try {
            // Run the Spring Boot application
            SpringApplication.run(YugabyteApplication.class, args);

            // Log after successful startup
            logger.info("YugabyteApplication started successfully.");
        } catch (Exception e) {
            // Log any exceptions during startup
            logger.error("Error starting YugabyteApplication", e);
        }
    }
}