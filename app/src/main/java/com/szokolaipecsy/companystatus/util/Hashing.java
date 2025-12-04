package com.szokolaipecsy.companystatus.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Hashing {

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String sha256Hex(String s) {
        return sha256(s);
    }

    public static String hashWithSalt(String raw, String saltHex) {
        return sha256Hex(saltHex + ":" + raw);
    }
}
