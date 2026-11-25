package com.example.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.MainActivity;
import com.example.R;
import com.example.api.ApiClient;
import com.example.databinding.FragmentLoginBinding;
import com.example.utils.Resource;
import com.example.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager = new SessionManager(requireContext());

        refreshServerUrlLabel();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnRegisterCustomer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_registerCustomer));
        binding.btnRegisterWorker.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_registerWorker));
        binding.btnServerSettings.setOnClickListener(v -> showServerUrlDialog());
    }

    private void attemptLogin() {
        String identifier = textOf(binding.etIdentifier);
        String password = textOf(binding.etPassword);

        if (TextUtils.isEmpty(identifier)) {
            showError("Please enter your email or mobile number.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password.");
            return;
        }

        binding.tvError.setVisibility(View.GONE);
        viewModel.login(identifier, password).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.LOADING) {
                setLoading(true);
            } else if (resource.status == Resource.Status.SUCCESS) {
                setLoading(false);
                if (getActivity() instanceof MainActivity main) {
                    main.routeUserByRole();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                setLoading(false);
                showError(resource.message);
            }
        });
    }

    private void showServerUrlDialog() {
        EditText input = new EditText(requireContext());
        input.setText(sessionManager.getBaseUrl());
        input.setHint("http://10.0.2.2:8080/ or http://192.168.x.x:8080/");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Spring Boot Server URL")
                .setMessage("Emulator default: http://10.0.2.2:8080/\nPhysical phone: http://<PC-LAN-IP>:8080/")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String entered = input.getText().toString().trim();
                    if (!entered.isEmpty()) {
                        sessionManager.setBaseUrl(entered);
                        ApiClient.reset();
                        refreshServerUrlLabel();
                    }
                })
                .setNeutralButton("Reset Emulator", (dialog, which) -> {
                    sessionManager.setBaseUrl(SessionManager.DEFAULT_EMULATOR_BASE_URL);
                    ApiClient.reset();
                    refreshServerUrlLabel();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshServerUrlLabel() {
        if (binding != null) {
            binding.tvCurrentServer.setText("Server: " + sessionManager.getBaseUrl());
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnRegisterCustomer.setEnabled(!loading);
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
