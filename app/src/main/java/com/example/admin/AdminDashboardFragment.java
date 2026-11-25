package com.example.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.databinding.FragmentAdminDashboardBinding;
import com.example.model.AdminModels;
import com.example.utils.Resource;

import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);
        binding.swipeRefresh.setOnRefreshListener(this::loadDashboard);
        loadDashboard();
    }

    private void loadDashboard() {
        viewModel.getDashboard().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
                binding.tvError.setVisibility(View.GONE);
            } else if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.swipeRefresh.setRefreshing(false);
                AdminModels.DashboardStats s = res.data;
                binding.tvTotalCustomers.setText(String.valueOf(s.totalCustomers));
                binding.tvTotalWorkers.setText(String.valueOf(s.totalWorkers));
                binding.tvVerifiedWorkers.setText(String.valueOf(s.verifiedWorkers));
                binding.tvPendingWorkers.setText(String.valueOf(s.pendingWorkers));
                binding.tvActiveJobs.setText(String.valueOf(s.activeJobs));
                binding.tvCompletedJobs.setText(String.valueOf(s.completedJobs));
                binding.tvCancelledJobs.setText(String.valueOf(s.cancelledJobs));
                binding.tvTodaysRevenue.setText(String.format(Locale.getDefault(), "₹%.0f", s.todaysRevenue));
                binding.tvPlatformCommission.setText(String.format(Locale.getDefault(), "₹%.0f", s.platformCommission));
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                binding.tvError.setText(res.message);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
