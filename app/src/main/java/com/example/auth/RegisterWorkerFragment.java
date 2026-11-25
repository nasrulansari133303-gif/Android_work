package com.example.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainActivity;
import com.example.databinding.FragmentRegisterWorkerBinding;
import com.example.model.AuthModels;
import com.example.model.ServiceModel;
import com.example.utils.Resource;

import java.util.ArrayList;
import java.util.List;

public class RegisterWorkerFragment extends Fragment {

    private FragmentRegisterWorkerBinding binding;
    private AuthViewModel viewModel;
    private final List<CheckBox> serviceCheckBoxes = new ArrayList<>();
    private String aadhaarDocUrl = "";
    private String otherDocUrl = "";
    private boolean pickingAadhaar = true;

    private final ActivityResultLauncher<PickVisualMediaRequest> docPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    uploadDocument(uri, pickingAadhaar);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterWorkerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        loadServices();

        binding.btnUploadAadhaar.setOnClickListener(v -> {
            pickingAadhaar = true;
            docPicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnUploadOtherDoc.setOnClickListener(v -> {
            pickingAadhaar = false;
            docPicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnRegisterWorker.setOnClickListener(v -> submitRegistration());
    }

    private void loadServices() {
        viewModel.loadServices().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.llServiceCheckboxes.removeAllViews();
                serviceCheckBoxes.clear();
                for (ServiceModel svc : res.data) {
                    CheckBox cb = new CheckBox(requireContext());
                    cb.setText(svc.name + " (Base ₹" + Math.round(svc.basePrice) + ")");
                    cb.setTag(svc.id);
                    cb.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
                    binding.llServiceCheckboxes.addView(cb);
                    serviceCheckBoxes.add(cb);
                }
            } else if (res.status == Resource.Status.ERROR) {
                showError("Could not load service categories: " + res.message);
            }
        });
    }

    private void uploadDocument(Uri uri, boolean isAadhaar) {
        binding.tvUploadedDocsStatus.setText("Uploading document...");
        viewModel.uploadFile(uri, "kyc").observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                if (isAadhaar) {
                    aadhaarDocUrl = res.data;
                } else {
                    otherDocUrl = res.data;
                }
                updateDocStatusLabel();
            } else {
                showError("Document upload failed: " + res.message);
                updateDocStatusLabel();
            }
        });
    }

    private void updateDocStatusLabel() {
        StringBuilder sb = new StringBuilder();
        if (!aadhaarDocUrl.isEmpty()) sb.append("Aadhaar attached");
        if (!otherDocUrl.isEmpty()) {
            if (sb.length() > 0) sb.append("  |  ");
            sb.append("Other KYC attached");
        }
        binding.tvUploadedDocsStatus.setText(sb.toString());
    }

    private void submitRegistration() {
        String fullName = textOf(binding.etFullName);
        String mobile = textOf(binding.etMobile);
        String email = textOf(binding.etEmail);
        String password = textOf(binding.etPassword);
        String address = textOf(binding.etAddress);
        String expStr = textOf(binding.etExperience);
        String aadhaar = textOf(binding.etAadhaar);
        String upiId = textOf(binding.etUpiId);

        if (TextUtils.isEmpty(fullName) || mobile.length() < 10 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter valid full name, mobile number, and email.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            showError("Address is required.");
            return;
        }

        List<Long> selectedIds = new ArrayList<>();
        for (CheckBox cb : serviceCheckBoxes) {
            if (cb.isChecked() && cb.getTag() instanceof Long id) {
                selectedIds.add(id);
            }
        }
        if (selectedIds.isEmpty()) {
            showError("Please select at least one service you provide.");
            return;
        }
        if (TextUtils.isEmpty(aadhaar)) {
            showError("Aadhaar number is required for KYC verification.");
            return;
        }

        int expYears = 0;
        try {
            if (!expStr.isEmpty()) expYears = Integer.parseInt(expStr);
        } catch (NumberFormatException ignored) {}

        binding.tvError.setVisibility(View.GONE);
        AuthModels.RegisterWorkerRequest req = new AuthModels.RegisterWorkerRequest();
        req.fullName = fullName;
        req.mobile = mobile;
        req.email = email;
        req.password = password;
        req.address = address;
        req.experienceYears = expYears;
        req.serviceIds = selectedIds;
        req.aadhaarNumber = aadhaar;
        req.aadhaarDocUrl = aadhaarDocUrl;
        req.otherDocUrl = otherDocUrl;
        req.upiId = upiId;
        req.bankAccountNumber = textOf(binding.etBankAccount);
        req.ifscCode = textOf(binding.etIfsc);

        viewModel.registerWorker(req).observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.status == Resource.Status.LOADING) {
                setLoading(true);
            } else if (res.status == Resource.Status.SUCCESS) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        "Application submitted! Pending Admin verification.", Toast.LENGTH_LONG).show();
                if (getActivity() instanceof MainActivity main) {
                    main.routeUserByRole();
                }
            } else if (res.status == Resource.Status.ERROR) {
                setLoading(false);
                showError(res.message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegisterWorker.setEnabled(!loading);
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
