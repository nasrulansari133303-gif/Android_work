package com.example.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.FragmentCustomerJobsBinding;
import com.example.model.JobModel;
import com.example.utils.Resource;
import com.example.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.List;

public class CustomerJobsFragment extends Fragment {

    private FragmentCustomerJobsBinding binding;
    private CustomerViewModel viewModel;
    private JobAdapter adapter;
    private final List<JobModel> allJobs = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);

        adapter = new JobAdapter(false, job -> {
            Bundle args = new Bundle();
            args.putLong("jobId", job.id);
            Navigation.findNavController(view).navigate(R.id.action_jobs_to_jobDetail, args);
        });
        binding.rvJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvJobs.setAdapter(adapter);

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipActive)) {
                currentFilter = "ACTIVE";
            } else if (checkedIds.contains(R.id.chipCompleted)) {
                currentFilter = "COMPLETED";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });

        binding.swipeRefresh.setOnRefreshListener(this::loadJobs);

        WebSocketManager.getInstance().getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event != null && ("JOB_ACCEPTED".equals(event.type)
                    || "JOB_STATUS_UPDATED".equals(event.type)
                    || "JOB_CANCELLED".equals(event.type))) {
                loadJobs();
            }
        });

        loadJobs();
    }

    private void loadJobs() {
        viewModel.getCustomerJobs().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
            } else if (res.status == Resource.Status.SUCCESS) {
                binding.swipeRefresh.setRefreshing(false);
                allJobs.clear();
                if (res.data != null) allJobs.addAll(res.data);
                applyFilter();
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter() {
        List<JobModel> filtered = new ArrayList<>();
        for (JobModel job : allJobs) {
            boolean isCompleted = "COMPLETED".equals(job.status)
                    || "PAYMENT".equals(job.status)
                    || "RATED".equals(job.status);
            boolean isCancelled = "CANCELLED".equals(job.status);
            if ("ACTIVE".equals(currentFilter)) {
                if (!isCompleted && !isCancelled) filtered.add(job);
            } else if ("COMPLETED".equals(currentFilter)) {
                if (isCompleted) filtered.add(job);
            } else {
                filtered.add(job);
            }
        }
        adapter.submitList(filtered);
        binding.llEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
