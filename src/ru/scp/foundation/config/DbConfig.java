package ru.scp.foundation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DbConfig {
    private static final String CONFIG_FILE = "config.properties";

    private final String dialect;
    private final String url;
    private final String user;
    private final String password;

    private DbConfig(Properties p) {
        this.dialect = required(p, "db.dialect").toLowerCase();
        this.url = required(p, "db.url");
        this.user = required(p, "db.user");
        this.password = p.getProperty("db.password", "");
        if (!dialect.equals("postgres") && !dialect.equals("firebird")) {
            throw new IllegalStateException(
                "db.dialect должен быть postgres или firebird, получено: " + dialect);
        }
    }

    public static DbConfig load() {
        Path path = Path.of(CONFIG_FILE);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "Не найден " + CONFIG_FILE + ". Скопируйте config.example.properties.");
        }
        Properties p = new Properties();
        try (var in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать " + CONFIG_FILE, e);
        }
        return new DbConfig(p);
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("В config.properties отсутствует: " + key);
        }
        return value;
    }

    public String dialect() { return dialect; }
    public String url()     { return url; }
    public String user()    { return user; }
    public String password(){ return password; }
}
