package com.example.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.MainActivity;
import com.example.R;
import com.example.databinding.FragmentAdminManageBinding;
import com.example.databinding.ItemAdminSimpleRowBinding;
import com.example.model.AdminModels;
import com.example.model.ServiceModel;
import com.example.utils.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminManageFragment extends Fragment {

    private enum Section { CUSTOMERS, SERVICES, PAYMENTS, COMPLAINTS }

    private FragmentAdminManageBinding binding;
    private AdminViewModel viewModel;
    private SimpleRowAdapter adapter;
    private Section currentSection = Section.CUSTOMERS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminManageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new SimpleRowAdapter();
        binding.rvManage.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvManage.setAdapter(adapter);

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.logoutAndReturnToLogin();
            }
        });

        binding.btnAddService.setOnClickListener(v -> showAddServiceDialog());

        binding.chipGroupSection.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipServices)) {
                currentSection = Section.SERVICES;
            } else if (checkedIds.contains(R.id.chipPayments)) {
                currentSection = Section.PAYMENTS;
            } else if (checkedIds.contains(R.id.chipComplaints)) {
                currentSection = Section.COMPLAINTS;
            } else {
                currentSection = Section.CUSTOMERS;
            }
            binding.btnAddService.setVisibility(currentSection == Section.SERVICES ? View.VISIBLE : View.GONE);
            loadSection();
        });

        binding.swipeRefresh.setOnRefreshListener(this::loadSection);
        loadSection();
    }

    private void loadSection() {
        switch (currentSection) {
            case CUSTOMERS -> loadCustomers();
            case SERVICES -> loadServices();
            case PAYMENTS -> loadPayments();
            case COMPLAINTS -> loadComplaints();
        }
    }

    private void loadCustomers() {
        viewModel.getCustomers(null).observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            binding.swipeRefresh.setRefreshing(res.status == Resource.Status.LOADING);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                List<RowModel> rows = new ArrayList<>();
                for (AdminModels.AdminCustomerItem c : res.data) {
                    rows.add(new RowModel(
                            c.fullName + " (" + c.mobile + ")",
                            c.email + " • Jobs: " + c.jobCount + " • " + (c.active ? "ACTIVE" : "BLOCKED"),
                            c.active ? "Block" : "Activate",
                            true,
                            () -> viewModel.setCustomerActive(c.userId, !c.active).observe(getViewLifecycleOwner(), r -> {
                                if (r != null && r.status == Resource.Status.SUCCESS) loadCustomers();
                            })
                    ));
                }
                adapter.submitList(rows);
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadServices() {
        viewModel.getServices().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            binding.swipeRefresh.setRefreshing(res.status == Resource.Status.LOADING);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                List<RowModel> rows = new ArrayList<>();
                for (ServiceModel s : res.data) {
                    rows.add(new RowModel(
                            s.name + " (₹" + Math.round(s.basePrice) + ")",
                            (s.description == null ? "" : s.description) + " • " + (s.active ? "ACTIVE" : "DISABLED"),
                            s.active ? "Disable" : "Enable",
                            true,
                            () -> viewModel.toggleService(s.id, !s.active).observe(getViewLifecycleOwner(), r -> {
                                if (r != null && r.status == Resource.Status.SUCCESS) loadServices();
                            })
                    ));
                }
                adapter.submitList(rows);
            }
        });
    }

    private void loadPayments() {
        viewModel.getPayments().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            binding.swipeRefresh.setRefreshing(res.status == Resource.Status.LOADING);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                List<RowModel> rows = new ArrayList<>();
                for (AdminModels.AdminPaymentItem p : res.data) {
                    boolean canRefund = "PAID".equalsIgnoreCase(p.status);
                    rows.add(new RowModel(
                            String.format(Locale.getDefault(), "%s • ₹%.0f (%s)", p.jobCode, p.amount, p.method),
                            String.format(Locale.getDefault(), "%s • Status: %s • Fee: ₹%.0f", p.customerName, p.status, p.commission),
                            canRefund ? "Refund" : p.status,
                            canRefund,
                            () -> viewModel.refundPayment(p.id).observe(getViewLifecycleOwner(), r -> {
                                if (r != null && r.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Payment refunded", Toast.LENGTH_SHORT).show();
                                    loadPayments();
                                }
                            })
                    ));
                }
                adapter.submitList(rows);
            }
        });
    }

    private void loadComplaints() {
        viewModel.getComplaints().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            binding.swipeRefresh.setRefreshing(res.status == Resource.Status.LOADING);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                List<RowModel> rows = new ArrayList<>();
                for (AdminModels.ComplaintItem c : res.data) {
                    boolean isResolved = "RESOLVED".equalsIgnoreCase(c.status);
                    rows.add(new RowModel(
                            c.subject + " [" + c.status + "]",
                            c.raisedBy + " (" + c.role + ") " + (c.jobCode != null ? "• " + c.jobCode : "")
                                    + "\n" + c.description,
                            isResolved ? "Resolved" : "Resolve",
                            !isResolved,
                            () -> showResolveComplaintDialog(c)
                    ));
                }
                adapter.submitList(rows);
            }
        });
    }

    private void showAddServiceDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(requireContext());
        etName.setHint("Service Name (e.g. CCTV Installation)");
        container.addView(etName);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Description");
        container.addView(etDesc);

        EditText etPrice = new EditText(requireContext());
        etPrice.setHint("Base Price (₹)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etPrice);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Service Category")
                .setView(container)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    double price = 299;
                    try {
                        price = Double.parseDouble(etPrice.getText().toString().trim());
                    } catch (Exception ignored) {}
                    if (name.isEmpty()) return;
                    viewModel.addService(name, desc, price).observe(getViewLifecycleOwner(), r -> {
                        if (r != null && r.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Service added!", Toast.LENGTH_SHORT).show();
                            loadServices();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResolveComplaintDialog(AdminModels.ComplaintItem c) {
        EditText etResponse = new EditText(requireContext());
        etResponse.setHint("Admin resolution response");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etResponse.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Resolve Complaint #" + c.id)
                .setMessage(c.description)
                .setView(etResponse)
                .setPositiveButton("Mark Resolved", (d, w) -> {
                    String resp = etResponse.getText().toString().trim();
                    viewModel.resolveComplaint(c.id, "RESOLVED", resp.isEmpty() ? "Resolved by Admin" : resp)
                            .observe(getViewLifecycleOwner(), r -> {
                                if (r != null && r.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Complaint resolved", Toast.LENGTH_SHORT).show();
                                    loadComplaints();
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

    private record RowModel(String title, String subtitle, String actionLabel, boolean actionEnabled, Runnable onAction) {}

    private static class SimpleRowAdapter extends RecyclerView.Adapter<SimpleRowAdapter.VH> {
        private final List<RowModel> items = new ArrayList<>();

        void submitList(List<RowModel> rows) {
            items.clear();
            items.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemAdminSimpleRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RowModel r = items.get(position);
            holder.binding.tvTitle.setText(r.title());
            holder.binding.tvSubtitle.setText(r.subtitle());
            holder.binding.btnAction.setText(r.actionLabel());
            holder.binding.btnAction.setEnabled(r.actionEnabled());
            holder.binding.btnAction.setOnClickListener(v -> {
                if (r.onAction() != null) r.onAction().run();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemAdminSimpleRowBinding binding;
            VH(ItemAdminSimpleRowBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
