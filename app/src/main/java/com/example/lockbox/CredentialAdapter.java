package com.example.lockbox;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lockbox.data.CredentialEntity;

import java.util.List;

public class CredentialAdapter extends RecyclerView.Adapter<CredentialAdapter.CredentialViewHolder> {

    private List<CredentialEntity> credentialList;

    public CredentialAdapter(List<CredentialEntity> credentialList) {
        this.credentialList = credentialList;
    }

    public void updateList(List<CredentialEntity> newList) {
        this.credentialList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CredentialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_credential, parent, false);
        return new CredentialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CredentialViewHolder holder, int position) {
        CredentialEntity credential = credentialList.get(position);
        holder.tvSite.setText(credential.siteOrApp);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CredentialDetailActivity.class);
            intent.putExtra("site", credential.siteOrApp);
            intent.putExtra("username", credential.username);
            intent.putExtra("password", credential.encryptedPassword);
            intent.putExtra("note", credential.note);
            intent.putExtra("id", credential.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return credentialList.size();
    }

    public static class CredentialViewHolder extends RecyclerView.ViewHolder {
        TextView tvSite;

        public CredentialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSite = itemView.findViewById(R.id.tvItemSite);
        }
    }
}