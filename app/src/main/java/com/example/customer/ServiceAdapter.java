package com.example.customer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemServiceCardBinding;
import com.example.model.ServiceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceVH> {

    public interface OnServiceClickListener {
        void onServiceClick(ServiceModel service);
    }

    private final List<ServiceModel> items = new ArrayList<>();
    private final OnServiceClickListener listener;

    public ServiceAdapter(OnServiceClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ServiceModel> services) {
        items.clear();
        if (services != null) {
            items.addAll(services);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceCardBinding binding = ItemServiceCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceVH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceVH holder, int position) {
        ServiceModel item = items.get(position);
        holder.binding.tvServiceName.setText(item.name);
        holder.binding.tvServicePrice.setText(String.format(Locale.getDefault(), "Starts ₹%.0f", item.basePrice));
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onServiceClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ServiceVH extends RecyclerView.ViewHolder {
        final ItemServiceCardBinding binding;

        ServiceVH(ItemServiceCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
