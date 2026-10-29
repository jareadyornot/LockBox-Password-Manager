package com.example.lockbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lockbox.data.CredentialEntity;
import com.example.lockbox.data.LockBoxDatabase;

import java.util.ArrayList;
import java.util.List;

public class CredentialsActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private CredentialAdapter adapter;
    private List<CredentialEntity> credentialList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credentials);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerViewCredentials);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CredentialAdapter(credentialList);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadCredentials);

        findViewById(R.id.btnAddCredential).setOnClickListener(v -> {
            Intent intent = new Intent(CredentialsActivity.this, AddCredentialActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

        // Bottom Navigation Listeners
        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(CredentialsActivity.this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(CredentialsActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCredentials();
    }

    private void loadCredentials() {
        String currentPin = Session.getDbKey();

        // Guard check: If session pin is empty, force user back to login instead of crashing
        if (currentPin == null || currentPin.isEmpty()) {
            Intent intent = new Intent(CredentialsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        new Thread(() -> {
            try {
                LockBoxDatabase db = LockBoxDatabase.getInstance(this, currentPin);
                List<CredentialEntity> list = db.credentialDao().getAllCredentials();

                runOnUiThread(() -> {
                    adapter.updateList(list);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Could not load credentials: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}