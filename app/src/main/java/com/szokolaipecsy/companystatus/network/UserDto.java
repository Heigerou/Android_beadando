package com.szokolaipecsy.companystatus.network;

public class UserDto {
    public String id;
    public String name;
    public String email;
    public String password;

    public UserDto() {}

    public UserDto(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
