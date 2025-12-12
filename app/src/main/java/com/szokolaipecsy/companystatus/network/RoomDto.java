package com.szokolaipecsy.companystatus.network;

// Ez egy DTO (Data Transfer Object)
// A szoba adatai így jönnek le az API-ról
// Nem adatbázis, csak hálózati adatcsomag
public class RoomDto {

    // A szoba egyedi azonosítója (szerveren)
    public String id;

    // A szoba neve
    public String name;

    // Rövid alcím
    public String subtitle;

    // Részletes leírás a szobáról
    public String description;

    // Megadja, hogy a szoba jelszóval védett-e
    // true -> jelszavas
    // false -> nyitott
    public Boolean isProtected;

    // Jelszó só (salt)
    // Biztonsági okból van
    public String passwordSalt;

    // Jelszó hash
    // A valódi jelszó soha nem szerepel itt
    public String passwordHash;
}
