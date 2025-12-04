package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "personal_phone")
    public String personalPhone;

    @ColumnInfo(name = "company_phone")
    public String companyPhone;

    @ColumnInfo(name = "internal_email")
    public String internalEmail;

    public UserProfile() {}

    public UserProfile(@NonNull String userId,
                       String name,
                       String email,
                       String personalPhone,
                       String companyPhone,
                       String internalEmail) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.personalPhone = personalPhone;
        this.companyPhone = companyPhone;
        this.internalEmail = internalEmail;
        this.displayName = null;
    }
}
