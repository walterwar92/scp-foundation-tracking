package ru.scp.foundation.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordHasher {

    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasswordHasher() {}

    /** Генерирует новую соль (16 байт, 32 hex-символа). */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RNG.nextBytes(saltBytes);
        return HEX.formatHex(saltBytes);
    }

    /** Считает SHA-256(salt || password) и возвращает 64 hex-символа. */
    public static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(HEX.parseHex(salt));
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 не поддерживается JVM", e);
        }
    }

    /** Сравнивает пароль с хешем + солью. */
    public static boolean verify(String salt, String password, String expectedHash) {
        return MessageDigest.isEqual(
            hash(salt, password).getBytes(StandardCharsets.US_ASCII),
            expectedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    /**
     * Утилита для генерации SQL-инсертов в seed-данные.
     * Запуск: java -cp out ru.scp.foundation.auth.PasswordHasher admin scp-foundation
     * Печатает на stdout SQL-строку с солью и хешем.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: PasswordHasher <login> <password>");
            System.exit(1);
        }
        String login = args[0];
        String password = args[1];
        String salt = generateSalt();
        String hash = hash(salt, password);
        System.out.println("-- " + login + " : " + password);
        System.out.println("-- salt = " + salt);
        System.out.println("-- hash = " + hash);
    }
}
