package com.example.hackathonprep.ui.notifications;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hackathonprep.R;

import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private List<Alert> alertList;

    public AlertsAdapter(List<Alert> alerts) {
        this.alertList = alerts;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alertList.get(position);
        holder.tvTitle.setText(alert.getTitle());
        holder.tvLocation.setText(alert.getLocation());
        holder.tvTime.setText(alert.getTime());

        // Set color indicator based on type
        Object type = alert.getType();
        if (type.equals("GBV")) {
            holder.colorIndicator.setBackgroundColor(Color.RED);
        } else if (type.equals("Crime")) {
            holder.colorIndicator.setBackgroundColor(0xFFFFA500); // Orange
        } else if (type.equals("Harassment")) {
            holder.colorIndicator.setBackgroundColor(Color.YELLOW);
        } else {
            holder.colorIndicator.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public void updateList(List<Alert> newList) {
        alertList = newList;
        notifyDataSetChanged();
    }


    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvTime;
        View colorIndicator;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvIncidentTitle);
            tvLocation = itemView.findViewById(R.id.tvIncidentLocation);
            tvTime = itemView.findViewById(R.id.tvIncidentTime);
            colorIndicator = itemView.findViewById(R.id.viewColorIndicator);
        }
    }
}
