package com.citytrip.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = (salt + ":" + password).getBytes(StandardCharsets.UTF_8);
            return toHex(digest.digest(input));
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
