package com.example.customer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.R;
import com.example.databinding.FragmentCustomerHomeBinding;
import com.example.model.JobModel;
import com.example.utils.Resource;
import com.example.utils.SessionManager;
import com.example.websocket.WebSocketManager;

public class CustomerHomeFragment extends Fragment {

    private FragmentCustomerHomeBinding binding;
    private CustomerViewModel viewModel;
    private ServiceAdapter adapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private Long activeJobId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        SessionManager session = new SessionManager(requireContext());

        String name = session.getFullName();
        binding.tvGreeting.setText(name.isEmpty() ? "Kaam-Mitra" : "Namaste, " + name);

        adapter = new ServiceAdapter(service -> {
            viewModel.setSelectedService(service);
            Navigation.findNavController(view).navigate(R.id.action_home_to_createJob);
        });
        binding.rvServices.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvServices.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            String q = binding.etSearchService.getText() == null ? "" : binding.etSearchService.getText().toString().trim();
            loadServices(q);
            loadActiveBanner();
        });

        binding.btnViewActiveJob.setOnClickListener(v -> {
            if (activeJobId != null) {
                Bundle args = new Bundle();
                args.putLong("jobId", activeJobId);
                Navigation.findNavController(v).navigate(R.id.action_home_to_jobDetail, args);
            }
        });

        binding.etSearchService.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String q = s == null ? "" : s.toString().trim();
                searchRunnable = () -> loadServices(q);
                searchHandler.postDelayed(searchRunnable, 350L);
            }
        });

        WebSocketManager.getInstance().getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event != null && ("JOB_ACCEPTED".equals(event.type) || "JOB_STATUS_UPDATED".equals(event.type))) {
                loadActiveBanner();
            }
        });

        loadServices(null);
        loadActiveBanner();
    }

    private void loadServices(String query) {
        viewModel.getServices(query).observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.tvErrorBanner.setVisibility(View.GONE);
            } else if (res.status == Resource.Status.SUCCESS) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                adapter.submitList(res.data);
            } else if (res.status == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                binding.tvErrorBanner.setText(res.message);
                binding.tvErrorBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadActiveBanner() {
        viewModel.getCustomerJobs().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null || res.status != Resource.Status.SUCCESS || res.data == null) return;
            JobModel active = null;
            for (JobModel job : res.data) {
                if (!"RATED".equals(job.status) && !"CANCELLED".equals(job.status) && !"PAYMENT".equals(job.status)) {
                    active = job;
                    break;
                }
            }
            if (active != null) {
                activeJobId = active.id;
                binding.cardActiveJobBanner.setVisibility(View.VISIBLE);
                binding.tvActiveBannerTitle.setText(active.jobCode + " • " + active.serviceName);
                binding.tvActiveBannerStatus.setText("Status: " + active.status
                        + (active.workerName != null ? " (" + active.workerName + ")" : ""));
            } else {
                activeJobId = null;
                binding.cardActiveJobBanner.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
