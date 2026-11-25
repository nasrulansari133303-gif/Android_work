package com.example.worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainActivity;
import com.example.databinding.FragmentWorkerProfileBinding;
import com.example.model.ServiceModel;
import com.example.model.WorkerModels;
import com.example.utils.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WorkerProfileFragment extends Fragment {

    private FragmentWorkerProfileBinding binding;
    private WorkerViewModel viewModel;
    private final List<CheckBox> serviceCheckBoxes = new ArrayList<>();
    private final Set<Long> currentWorkerServiceIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkerViewModel.class);

        binding.btnSaveServices.setOnClickListener(v -> saveSelectedServices());
        binding.btnUpdateProfile.setOnClickListener(v -> updateUpi());
        binding.btnSubmitComplaint.setOnClickListener(v -> showComplaintDialog());
        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.logoutAndReturnToLogin();
            }
        });

        loadProfile();
    }

    private void loadProfile() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null || res.status != Resource.Status.SUCCESS || res.data == null) return;
            WorkerModels.WorkerProfile p = res.data;
            binding.tvName.setText(p.fullName + " (" + p.verificationStatus + ")");
            binding.tvContact.setText(p.email + "  •  " + p.mobile);
            binding.tvMetrics.setText(String.format(Locale.getDefault(),
                    "Rating: %.1f ★ (%d)  •  Acceptance: %.0f%%  •  Completion: %.0f%%",
                    p.averageRating, p.totalRatings, p.acceptanceRate, p.completionRate));
            binding.etUpiId.setText(p.upiId);

            currentWorkerServiceIds.clear();
            if (p.services != null) {
                for (WorkerModels.ServiceRef ref : p.services) {
                    currentWorkerServiceIds.add(ref.id);
                }
            }
            loadAllServices();
        });
    }

    private void loadAllServices() {
        viewModel.loadAllServices().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null || res.status != Resource.Status.SUCCESS || res.data == null) return;
            binding.llServicesContainer.removeAllViews();
            serviceCheckBoxes.clear();
            for (ServiceModel svc : res.data) {
                CheckBox cb = new CheckBox(requireContext());
                cb.setText(svc.name + " (Base ₹" + Math.round(svc.basePrice) + ")");
                cb.setTag(svc.id);
                cb.setChecked(currentWorkerServiceIds.contains(svc.id));
                cb.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
                binding.llServicesContainer.addView(cb);
                serviceCheckBoxes.add(cb);
            }
        });
    }

    private void saveSelectedServices() {
        List<Long> selected = new ArrayList<>();
        for (CheckBox cb : serviceCheckBoxes) {
            if (cb.isChecked() && cb.getTag() instanceof Long id) {
                selected.add(id);
            }
        }
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one service", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.updateServices(selected).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Services updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUpi() {
        String upi = binding.etUpiId.getText() == null ? "" : binding.etUpiId.getText().toString().trim();
        viewModel.updateProfile(Map.of("upiId", upi)).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS) {
                Toast.makeText(requireContext(), "Payout UPI updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showComplaintDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etSubject = new EditText(requireContext());
        etSubject.setHint("Subject");
        container.addView(etSubject);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Describe your issue...");
        container.addView(etDesc);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Support Complaint")
                .setView(container)
                .setPositiveButton("Submit", (d, w) -> {
                    String s = etSubject.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (s.isEmpty() || desc.isEmpty()) return;
                    viewModel.submitComplaint(null, s, desc).observe(getViewLifecycleOwner(), res -> {
                        if (res != null && res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Complaint submitted", Toast.LENGTH_SHORT).show();
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
}
