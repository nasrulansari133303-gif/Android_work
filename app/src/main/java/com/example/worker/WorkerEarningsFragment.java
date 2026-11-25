package com.example.worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.FragmentWorkerEarningsBinding;
import com.example.databinding.ItemEarningCardBinding;
import com.example.model.WorkerModels;
import com.example.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkerEarningsFragment extends Fragment {

    private FragmentWorkerEarningsBinding binding;
    private WorkerViewModel viewModel;
    private EarningAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerEarningsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkerViewModel.class);

        adapter = new EarningAdapter();
        binding.rvEarnings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEarnings.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadEarnings);

        loadEarnings();
    }

    private void loadEarnings() {
        viewModel.getEarnings().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
            } else if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.swipeRefresh.setRefreshing(false);
                binding.tvToday.setText(String.format(Locale.getDefault(), "₹%.0f", res.data.todayEarnings));
                binding.tvWeek.setText(String.format(Locale.getDefault(), "₹%.0f", res.data.weekEarnings));
                binding.tvMonth.setText(String.format(Locale.getDefault(), "₹%.0f", res.data.monthEarnings));
                adapter.submitList(res.data.items);
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class EarningAdapter extends RecyclerView.Adapter<EarningAdapter.VH> {
        private final List<WorkerModels.EarningItem> items = new ArrayList<>();

        void submitList(List<WorkerModels.EarningItem> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemEarningCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            WorkerModels.EarningItem e = items.get(position);
            holder.binding.tvJobService.setText(e.jobCode + " • " + e.serviceName);
            holder.binding.tvNetEarning.setText(String.format(Locale.getDefault(), "+₹%.0f", e.netEarning));
            holder.binding.tvBreakdown.setText(String.format(Locale.getDefault(),
                    "%s  •  Gross: ₹%.0f  |  Fee: ₹%.0f  |  %s",
                    e.customerName, e.grossAmount, e.platformCommission, e.paymentStatus));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemEarningCardBinding binding;
            VH(ItemEarningCardBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
