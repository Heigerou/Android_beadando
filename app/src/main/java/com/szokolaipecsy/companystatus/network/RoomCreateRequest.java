package com.szokolaipecsy.companystatus.network;

// Ez egy request osztály
// Akkor használjuk, amikor új szobát hozunk létre az API-n keresztül
public class RoomCreateRequest {

    // A szoba neve
    public String name;

    // Rövid alcím a szobához
    public String subtitle;

    // Részletes leírás a szobáról
    public String description;

    // Megadja, hogy a szoba védett-e jelszóval
    // true -> jelszavas
    // false -> nyitott
    public Boolean isProtected;

    // Jelszó só (salt)
    // Biztonsági okból használjuk
    public String passwordSalt;

    // A jelszó hash-e
    // A valódi jelszót nem küldjük el
    public String passwordHash;

    // Konstruktor
    // Itt adjuk meg a szoba létrehozásához szükséges adatokat
    public RoomCreateRequest(String name,
                             String subtitle,
                             String description,
                             Boolean isProtected,
                             String passwordSalt,
                             String passwordHash) {
        this.name = name;
        this.subtitle = subtitle;
        this.description = description;
        this.isProtected = isProtected;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }
}
