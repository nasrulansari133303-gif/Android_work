package com.example.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.FragmentAdminJobsBinding;
import com.example.databinding.ItemAdminJobBinding;
import com.example.model.AdminModels;
import com.example.model.JobModel;
import com.example.utils.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminJobsFragment extends Fragment {

    private FragmentAdminJobsBinding binding;
    private AdminViewModel viewModel;
    private AdminJobAdapter adapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new AdminJobAdapter();
        binding.rvAdminJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminJobs.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadJobs);

        binding.etSearchJob.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> loadJobs();
                searchHandler.postDelayed(searchRunnable, 350L);
            }
        });

        loadJobs();
    }

    private void loadJobs() {
        String q = binding != null && binding.etSearchJob.getText() != null
                ? binding.etSearchJob.getText().toString().trim() : "";
        viewModel.getJobs(q.isEmpty() ? null : q, null).observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.swipeRefresh.setRefreshing(true);
            } else if (res.status == Resource.Status.SUCCESS) {
                binding.swipeRefresh.setRefreshing(false);
                adapter.submitList(res.data);
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showReassignDialog(JobModel job) {
        viewModel.getWorkers("VERIFIED").observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null && !res.data.isEmpty()) {
                List<AdminModels.AdminWorkerItem> workers = res.data;
                String[] names = new String[workers.size()];
                for (int i = 0; i < workers.size(); i++) {
                    AdminModels.AdminWorkerItem w = workers.get(i);
                    names[i] = w.fullName + (w.online ? " [ONLINE]" : " [OFFLINE]") + " • " + w.mobile;
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Reassign " + job.jobCode)
                        .setItems(names, (d, which) -> {
                            AdminModels.AdminWorkerItem chosen = workers.get(which);
                            viewModel.reassignJob(job.id, chosen.workerId).observe(getViewLifecycleOwner(), r -> {
                                if (r != null && r.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Reassigned to " + chosen.fullName, Toast.LENGTH_SHORT).show();
                                    loadJobs();
                                } else if (r != null && r.status == Resource.Status.ERROR) {
                                    Toast.makeText(requireContext(), r.message, Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .show();
            } else {
                Toast.makeText(requireContext(), "No verified workers found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCancelDialog(JobModel job) {
        EditText input = new EditText(requireContext());
        input.setHint("Admin cancellation reason");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel " + job.jobCode)
                .setView(input)
                .setPositiveButton("Cancel Job", (d, w) -> {
                    viewModel.cancelJob(job.id, input.getText().toString().trim())
                            .observe(getViewLifecycleOwner(), res -> {
                                if (res != null && res.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Job cancelled", Toast.LENGTH_SHORT).show();
                                    loadJobs();
                                } else if (res != null && res.status == Resource.Status.ERROR) {
                                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    private class AdminJobAdapter extends RecyclerView.Adapter<AdminJobAdapter.VH> {
        private final List<JobModel> items = new ArrayList<>();

        void submitList(List<JobModel> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemAdminJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            JobModel job = items.get(position);
            holder.binding.tvJobHeader.setText(job.jobCode + " • " + job.serviceName + " [" + job.status + "]");
            double amt = job.finalAmount > 0 ? job.finalAmount : job.estimatedBudget;
            holder.binding.tvJobInfo.setText(String.format(Locale.getDefault(),
                    "Customer: %s  |  Worker: %s\n%s  •  ₹%.0f",
                    job.customerName,
                    job.workerName != null ? job.workerName : "Unassigned",
                    job.address, amt));

            boolean terminal = "COMPLETED".equals(job.status)
                    || "PAYMENT".equals(job.status)
                    || "RATED".equals(job.status)
                    || "CANCELLED".equals(job.status);

            holder.binding.btnReassign.setEnabled(!terminal);
            holder.binding.btnCancelJob.setEnabled(!terminal);
            holder.binding.btnReassign.setOnClickListener(v -> showReassignDialog(job));
            holder.binding.btnCancelJob.setOnClickListener(v -> showCancelDialog(job));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemAdminJobBinding binding;
            VH(ItemAdminJobBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
