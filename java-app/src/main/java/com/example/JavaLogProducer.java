package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class JavaLogProducer {
    private JavaLogProducer() {
    }

    public static void main(String[] args) {
        String logPathEnv = System.getenv().getOrDefault("JAVA_LOG_PATH", "/logs/java-app.log");
    double intervalSeconds = parseIntervalSeconds(System.getenv("LOG_INTERVAL_SECONDS"));
        String appName = System.getenv().getOrDefault("APP_NAME", "java-log-producer");

        Path logPath = Paths.get(logPathEnv);
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory for " + logPathEnv, e);
        }

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Random random = new Random();
        String[] levels = new String[]{"INFO", "DEBUG", "WARN", "ERROR"};

    System.out.printf("Starting Java log producer. Writing to %s every %.3f seconds.%n", logPathEnv, intervalSeconds);

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println("Java log producer stopping...")));

        try (BufferedWriter writer = Files.newBufferedWriter(
                logPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE)) {

            while (!Thread.currentThread().isInterrupted()) {
                Map<String, Object> logEntry = Map.of(
                        "timestamp", OffsetDateTime.now(),
                        "level", levels[random.nextInt(levels.length)],
                        "app", appName,
                        "message", "Completed background job",
                        "durationMs", random.nextInt(600) + 50,
                        "customerId", random.nextInt(500) + 500,
                        "tags", List.of("example", "java")
                );

                String json = mapper.writeValueAsString(logEntry);
                writer.write(json);
                writer.newLine();
                writer.flush();
                System.out.println(json);

                try {
                    long delayMillis = Math.round(intervalSeconds * 1000.0);
                    Thread.sleep(Math.max(delayMillis, 1L));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log entry", e);
        }

        System.out.println("Java log producer stopped.");
    }

    private static double parseIntervalSeconds(String intervalEnv) {
        if (intervalEnv == null) {
            return 0.1d;
        }
        try {
            double parsed = Double.parseDouble(intervalEnv);
            return Math.max(parsed, 0.1d);
        } catch (NumberFormatException ex) {
            return 0.1d;
        }
    }
}
