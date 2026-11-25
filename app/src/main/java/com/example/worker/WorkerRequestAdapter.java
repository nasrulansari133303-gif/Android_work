package com.example.worker;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemWorkerRequestCardBinding;
import com.example.model.JobModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkerRequestAdapter extends RecyclerView.Adapter<WorkerRequestAdapter.RequestVH> {

    public interface RequestActionListener {
        void onAccept(JobModel job);
        void onReject(JobModel job);
    }

    private final List<JobModel> items = new ArrayList<>();
    private final RequestActionListener listener;

    public WorkerRequestAdapter(RequestActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<JobModel> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RequestVH(ItemWorkerRequestCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RequestVH holder, int position) {
        JobModel job = items.get(position);
        holder.binding.tvServiceName.setText(job.serviceName);
        holder.binding.tvDistance.setText(job.distanceKm != null
                ? String.format(Locale.getDefault(), "%.1f km", job.distanceKm)
                : "Nearby");
        holder.binding.tvCustomerAndProblem.setText(
                (job.customerName != null ? job.customerName : "Customer")
                        + " • " + job.problemTitle + "\n" + job.address);
        double estEarning = Math.round(job.estimatedBudget * 0.90);
        holder.binding.tvEarning.setText(String.format(Locale.getDefault(), "Est. Payout: ₹%.0f", estEarning));

        holder.binding.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(job);
        });
        holder.binding.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(job);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RequestVH extends RecyclerView.ViewHolder {
        final ItemWorkerRequestCardBinding binding;

        RequestVH(ItemWorkerRequestCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
