package com.example.floodrescue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Report> reportList;

    public HistoryAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_report, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.tvDescription.setText(report.getDescription());
        holder.tvType.setText(report.getType());
        holder.tvTimestamp.setText(formatDate(report.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvType, tvTimestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvHistoryDescription);
            tvType = itemView.findViewById(R.id.tvHistoryType);
            tvTimestamp = itemView.findViewById(R.id.tvHistoryTimestamp);
        }
    }
}