package com.example.lockbox.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CredentialDao {

    @Query("SELECT * FROM credentials ORDER BY siteOrApp ASC")
    List<CredentialEntity> getAllCredentials();

    @Query("SELECT * FROM credentials WHERE id = :id")
    CredentialEntity getCredentialById(long id);

    @Query("SELECT * FROM credentials WHERE siteOrApp LIKE '%' || :searchQuery || '%' OR username LIKE '%' || :searchQuery || '%'")
    List<CredentialEntity> searchCredentials(String searchQuery);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCredential(CredentialEntity credential);

    @Update
    void updateCredential(CredentialEntity credential);

    @Delete
    void deleteCredential(CredentialEntity credential);
}