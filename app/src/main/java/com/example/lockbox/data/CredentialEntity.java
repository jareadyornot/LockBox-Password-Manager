package com.example.lockbox.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "credentials")
public class CredentialEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String siteOrApp;
    public String username;
    public String encryptedPassword;
    public String note;
}