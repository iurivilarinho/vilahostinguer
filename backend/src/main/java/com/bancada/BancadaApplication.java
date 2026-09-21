package com.bancada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BancadaApplication {

    public static void main(String[] args) throws IOException {
        // SQLite does not create missing folders, and the datasource starts before any bean could.
        String dataDir = System.getenv().getOrDefault("BANCADA_DATA", Paths.get(System.getProperty("user.home"), ".bancada").toString());
        Files.createDirectories(Paths.get(dataDir));
        String database = System.getenv("BANCADA_DB");
        if (database != null && !database.isBlank()) {
            Path parent = Paths.get(database).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
        SpringApplication.run(BancadaApplication.class, args);
    }
}
