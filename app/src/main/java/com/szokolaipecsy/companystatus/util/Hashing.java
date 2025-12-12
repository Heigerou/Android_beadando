package com.szokolaipecsy.companystatus.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Hashing segédosztály
// Célja: jelszavak és érzékeny adatok biztonságos hash-elése
// Az appban főleg szobák jelszavához használjuk (nem plain text!)
public class Hashing {

    // Egyszerű SHA-256 hash függvény
    // String -> hexadecimális hash
    public static String sha256(String input) {
        try {
            // SHA-256 algoritmus inicializálása
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // String -> byte[] UTF-8 kódolással
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // byte[] -> hex string (pl. a3f1...)
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));

            return sb.toString();

        } catch (Exception e) {
            // ha bármi hiba van, üres stringet adunk vissza
            return "";
        }
    }

    // Alias / olvashatóbb név
    // funkcionálisan ugyanaz, mint a sha256()
    public static String sha256Hex(String s) {
        return sha256(s);
    }

    // Hash sóval (salt)
    // raw = jelszó
    // saltHex = véletlen generált só (hex string)
    //
    // Formátum: salt:password -> SHA-256
    // Ez megakadályozza az egyszerű rainbow table támadásokat
    public static String hashWithSalt(String raw, String saltHex) {
        return sha256Hex(saltHex + ":" + raw);
    }
}
