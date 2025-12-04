package com.szokolaipecsy.companystatus.network;

public class RoomCreateRequest {

    public String name;
    public String subtitle;
    public String description;

    public Boolean isProtected;
    public String passwordSalt;
    public String passwordHash;

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
