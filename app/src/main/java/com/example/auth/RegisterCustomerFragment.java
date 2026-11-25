package com.example.auth;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainActivity;
import com.example.databinding.FragmentRegisterCustomerBinding;
import com.example.model.AuthModels;
import com.example.utils.Resource;

public class RegisterCustomerFragment extends Fragment {

    private FragmentRegisterCustomerBinding binding;
    private AuthViewModel viewModel;
    private String uploadedPhotoUrl = "";

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    binding.ivProfilePreview.setImageURI(uri);
                    uploadProfilePhoto(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterCustomerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnPickPhoto.setOnClickListener(v ->
                photoPicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.btnRegister.setOnClickListener(v -> submitRegistration());
    }

    private void uploadProfilePhoto(Uri uri) {
        binding.btnPickPhoto.setEnabled(false);
        binding.btnPickPhoto.setText("Uploading...");
        viewModel.uploadFile(uri, "profiles").observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            binding.btnPickPhoto.setEnabled(true);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                uploadedPhotoUrl = res.data;
                binding.btnPickPhoto.setText("Photo Uploaded");
                Toast.makeText(requireContext(), "Profile photo uploaded", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnPickPhoto.setText("Upload Profile Photo");
                showError(res.message);
            }
        });
    }

    private void submitRegistration() {
        String fullName = textOf(binding.etFullName);
        String mobile = textOf(binding.etMobile);
        String email = textOf(binding.etEmail);
        String password = textOf(binding.etPassword);
        String address = textOf(binding.etAddress);

        if (TextUtils.isEmpty(fullName)) {
            showError("Full name is required.");
            return;
        }
        if (mobile.length() < 10) {
            showError("Enter a valid 10+ digit mobile number.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter a valid email address.");
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

        binding.tvError.setVisibility(View.GONE);
        AuthModels.RegisterCustomerRequest req = new AuthModels.RegisterCustomerRequest();
        req.fullName = fullName;
        req.mobile = mobile;
        req.email = email;
        req.password = password;
        req.address = address;
        req.profilePhotoUrl = uploadedPhotoUrl;

        viewModel.registerCustomer(req).observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            if (res.status == Resource.Status.LOADING) {
                setLoading(true);
            } else if (res.status == Resource.Status.SUCCESS) {
                setLoading(false);
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
        binding.btnRegister.setEnabled(!loading);
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
