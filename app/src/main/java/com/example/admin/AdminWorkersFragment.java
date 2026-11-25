package com.example.admin;

import android.os.Bundle;
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

import com.example.R;
import com.example.databinding.FragmentAdminWorkersBinding;
import com.example.databinding.ItemAdminWorkerBinding;
import com.example.model.AdminModels;
import com.example.utils.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminWorkersFragment extends Fragment {

    private FragmentAdminWorkersBinding binding;
    private AdminViewModel viewModel;
    private WorkerListAdapter adapter;
    private String statusFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminWorkersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new WorkerListAdapter();
        binding.rvAdminWorkers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAdminWorkers.setAdapter(adapter);

        binding.chipGroupWorkerStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipPending)) {
                statusFilter = "PENDING_VERIFICATION";
            } else if (checkedIds.contains(R.id.chipVerified)) {
                statusFilter = "VERIFIED";
            } else {
                statusFilter = null;
            }
            loadWorkers();
        });

        binding.swipeRefresh.setOnRefreshListener(this::loadWorkers);
        loadWorkers();
    }

    private void loadWorkers() {
        viewModel.getWorkers(statusFilter).observe(getViewLifecycleOwner(), res -> {
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

    private void showWorkerKycDialog(AdminModels.AdminWorkerItem worker) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(worker.fullName).append("\n");
        sb.append("Mobile: ").append(worker.mobile).append("\n");
        sb.append("Experience: ").append(worker.experienceYears).append(" yrs\n");
        sb.append("Services: ").append(worker.services != null ? String.join(", ", worker.services) : "-").append("\n\n");
        sb.append("KYC Documents:\n");
        if (worker.documents == null || worker.documents.isEmpty()) {
            sb.append("No documents uploaded");
        } else {
            for (AdminModels.WorkerDocItem d : worker.documents) {
                sb.append("• ").append(d.type).append(" #").append(d.number != null ? d.number : "-");
                if (d.url != null && !d.url.isEmpty()) sb.append(" (").append(d.url).append(")");
                sb.append("\n");
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Worker KYC Details")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showRejectDialog(AdminModels.AdminWorkerItem worker) {
        EditText input = new EditText(requireContext());
        input.setHint("Rejection reason (e.g. Blurry Aadhaar)");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reject " + worker.fullName)
                .setView(input)
                .setPositiveButton("Reject", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    viewModel.rejectWorker(worker.workerId, reason.isEmpty() ? "KYC rejected by Admin" : reason)
                            .observe(getViewLifecycleOwner(), res -> {
                                if (res != null && res.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Worker rejected", Toast.LENGTH_SHORT).show();
                                    loadWorkers();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class WorkerListAdapter extends RecyclerView.Adapter<WorkerListAdapter.VH> {
        private final List<AdminModels.AdminWorkerItem> items = new ArrayList<>();

        void submitList(List<AdminModels.AdminWorkerItem> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemAdminWorkerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AdminModels.AdminWorkerItem w = items.get(position);
            holder.binding.tvWorkerName.setText(w.fullName + " (" + w.mobile + ")");
            holder.binding.tvVerificationStatus.setText(w.verificationStatus);
            String services = w.services == null ? "" : String.join(", ", w.services);
            holder.binding.tvDetails.setText(String.format(Locale.getDefault(),
                    "Services: %s\nExp: %d yrs  •  Rating: %.1f ★  •  Jobs: %d  •  Docs: %d",
                    services, w.experienceYears, w.averageRating, w.completedJobsCount,
                    w.documents == null ? 0 : w.documents.size()));

            holder.binding.getRoot().setOnClickListener(v -> showWorkerKycDialog(w));

            boolean isVerified = "VERIFIED".equalsIgnoreCase(w.verificationStatus);
            boolean isSuspended = "SUSPENDED".equalsIgnoreCase(w.verificationStatus) || !w.active;

            holder.binding.btnVerify.setEnabled(!isVerified);
            holder.binding.btnVerify.setOnClickListener(v ->
                    viewModel.verifyWorker(w.workerId).observe(getViewLifecycleOwner(), res -> {
                        if (res != null && res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Worker verified", Toast.LENGTH_SHORT).show();
                            loadWorkers();
                        }
                    }));

            holder.binding.btnReject.setOnClickListener(v -> showRejectDialog(w));

            holder.binding.btnSuspend.setText(isSuspended ? "Activate" : "Suspend");
            holder.binding.btnSuspend.setOnClickListener(v -> {
                var call = isSuspended ? viewModel.activateWorker(w.workerId) : viewModel.suspendWorker(w.workerId);
                call.observe(getViewLifecycleOwner(), res -> {
                    if (res != null && res.status == Resource.Status.SUCCESS) {
                        Toast.makeText(requireContext(), isSuspended ? "Activated" : "Suspended", Toast.LENGTH_SHORT).show();
                        loadWorkers();
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemAdminWorkerBinding binding;
            VH(ItemAdminWorkerBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
