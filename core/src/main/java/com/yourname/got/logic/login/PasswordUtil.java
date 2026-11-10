package com.yourname.got.logic.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted SHA-256 password storage.
 *
 * Stored format: {@code sha256$<salt hex>$<hash hex>} (no commas, so it is safe inside the CSV user index).
 * Passwords stored by older versions (plain text) are still accepted by {@link #matches} and are
 * upgraded to the hashed format the next time the user logs in.
 *
 * Note: the game rules require 4-digit passwords, so hashing cannot make them strong
 * (only 10,000 possibilities); it just stops them from being readable in the data files.
 */
public final class PasswordUtil {
    private static final String PREFIX = "sha256$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static String hash(String password) {
        byte[] salt = new byte[8];
        RANDOM.nextBytes(salt);
        String saltHex = toHex(salt);
        return PREFIX + saltHex + "$" + digest(saltHex, password);
    }

    public static boolean matches(String input, String stored) {
        if (input == null || stored == null) return false;
        if (!isHashed(stored)) {
            return stored.equals(input); // legacy plain-text entry
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 3) return false;
        return constantTimeEquals(parts[2], digest(parts[1], input));
    }

    private static String digest(String saltHex, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest((saltHex + ":" + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
