package com.example.lockbox.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CredentialDao {

    @Query("SELECT * FROM credentials")
    List<CredentialEntity> getAllCredentials();

    @Query("SELECT * FROM credentials WHERE siteOrApp LIKE '%' || :searchQuery || '%' OR username LIKE '%' || :searchQuery || '%'")
    List<CredentialEntity> searchCredentials(String searchQuery);

    @Insert
    void insertCredential(CredentialEntity credential);

    @Delete
    void deleteCredential(CredentialEntity credential);
}