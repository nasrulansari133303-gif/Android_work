package com.example.customer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.databinding.ItemJobCardBinding;
import com.example.model.JobModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobVH> {

    public interface OnJobClickListener {
        void onJobClick(JobModel job);
    }

    private final List<JobModel> items = new ArrayList<>();
    private final boolean workerView;
    private final OnJobClickListener listener;

    public JobAdapter(boolean workerView, OnJobClickListener listener) {
        this.workerView = workerView;
        this.listener = listener;
    }

    public void submitList(List<JobModel> jobs) {
        items.clear();
        if (jobs != null) {
            items.addAll(jobs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JobVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobCardBinding binding = ItemJobCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new JobVH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull JobVH holder, int position) {
        JobModel job = items.get(position);
        holder.binding.tvJobCode.setText(job.jobCode + "  •  " + (job.preferredDate != null ? job.preferredDate : ""));
        holder.binding.tvStatus.setText(job.status);
        holder.binding.tvServiceAndProblem.setText(job.serviceName + " — " + job.problemTitle);
        holder.binding.tvAddress.setText(job.address);

        if (workerView) {
            holder.binding.tvWorkerInfo.setText("Customer: " + (job.customerName != null ? job.customerName : ""));
        } else {
            String workerText = job.workerName != null && !job.workerName.isEmpty()
                    ? "Partner: " + job.workerName
                    : "Finding nearby verified worker...";
            holder.binding.tvWorkerInfo.setText(workerText);
        }

        double amount = job.finalAmount > 0 ? job.finalAmount : job.estimatedBudget;
        holder.binding.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));

        int statusColor = R.color.km_saffron_dark;
        if ("COMPLETED".equals(job.status) || "PAYMENT".equals(job.status) || "RATED".equals(job.status)) {
            statusColor = R.color.km_teal;
        } else if ("CANCELLED".equals(job.status)) {
            statusColor = R.color.km_error;
        }
        holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.binding.getRoot().getContext(), statusColor));

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onJobClick(job);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class JobVH extends RecyclerView.ViewHolder {
        final ItemJobCardBinding binding;

        JobVH(ItemJobCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
