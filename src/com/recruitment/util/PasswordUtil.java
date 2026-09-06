package com.recruitment.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise Password Hashing Utility using SHA-256 with Cryptographic Salt.
 * Strictly prevents plain-text password storage.
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte cryptographically secure random salt encoded in Base64.
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Hashes a plain password with a salt using SHA-256.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) password = "";
        if (salt == null) salt = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies if an entered password matches the stored hash when combined with the salt.
     */
    public static boolean verifyPassword(String inputPassword, String salt, String expectedHash) {
        if (inputPassword == null || expectedHash == null) return false;
        String calculated = hashPassword(inputPassword, salt);
        return calculated.equalsIgnoreCase(expectedHash);
    }
}
