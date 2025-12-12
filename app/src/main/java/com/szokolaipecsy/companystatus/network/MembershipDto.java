package com.szokolaipecsy.companystatus.network;

// Ez egy DTO (Data Transfer Object)
// A szerverrel való kommunikációhoz használjuk
// Amit az API küld vagy fogad, az ebben az osztályban van
public class MembershipDto {

    // A membership egyedi azonosítója (szerveren)
    public String id;

    // Annak a szobának az ID-ja, amihez tartozik
    public String roomId;

    // A felhasználó ID-ja
    public String userId;

    // A felhasználó szerepe a szobában
    // pl. member, admin
    public String role;

    // Mikor csatlakozott a szobához
    // szerver küldi string formában
    public String joinedAt;

    // Mikor volt aktív utoljára
    public String lastActiveAt;

    // A felhasználó megjelenített neve
    // ez megy ki és jön vissza az API-ból
    public String userName;

    // Rövid státusz szöveg
    // pl. "Online", "Busy"
    public String statusText;

    // Elérhetőség állapota
    // pl. AVAILABLE / BUSY / AWAY
    public String availability;

}
