package com.szokolaipecsy.companystatus.network;

// Ez egy DTO (Data Transfer Object)
// A felhasználó adatait így küldjük el vagy kapjuk vissza az API-tól
public class UserDto {

    // Felhasználó egyedi azonosítója (szerveren)
    public String id;

    // Felhasználó neve
    public String name;

    // Felhasználó email címe
    public String email;

    // Jelszó
    // Regisztrációnál küldjük el a szervernek
    public String password;

    // Üres konstruktor
    // A Gson-nak kell, amikor az API válaszát objektummá alakítja
    public UserDto() {}

    // Konstruktor regisztrációhoz
    // Itt adjuk meg az új felhasználó adatait
    public UserDto(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
