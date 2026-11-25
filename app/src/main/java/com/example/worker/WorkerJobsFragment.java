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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.customer.JobAdapter;
import com.example.databinding.FragmentWorkerJobsBinding;
import com.example.utils.Resource;

public class WorkerJobsFragment extends Fragment {

    private FragmentWorkerJobsBinding binding;
    private WorkerViewModel viewModel;
    private JobAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkerViewModel.class);

        adapter = new JobAdapter(true, job -> {
            Bundle args = new Bundle();
            args.putLong("jobId", job.id);
            Navigation.findNavController(view).navigate(R.id.action_workerJobs_to_activeJob, args);
        });
        binding.rvWorkerJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWorkerJobs.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadJobs);

        loadJobs();
    }

    private void loadJobs() {
        viewModel.getWorkerJobs().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
            } else if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.swipeRefresh.setRefreshing(false);
                adapter.submitList(res.data.myJobs);
                binding.tvEmpty.setVisibility(
                        (res.data.myJobs == null || res.data.myJobs.isEmpty()) ? View.VISIBLE : View.GONE);
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
}
