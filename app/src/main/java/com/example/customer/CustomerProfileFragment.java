package com.example.customer;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.MainActivity;
import com.example.R;
import com.example.databinding.FragmentCustomerProfileBinding;
import com.example.location.LocationHelper;
import com.example.model.AddressModel;
import com.example.model.AdminModels;
import com.example.utils.Resource;
import com.example.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class CustomerProfileFragment extends Fragment {

    private FragmentCustomerProfileBinding binding;
    private CustomerViewModel viewModel;
    private LocationHelper locationHelper;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationHelper.hasLocationPermission(requireContext())) {
                    saveCurrentGpsAddress();
                } else {
                    Toast.makeText(requireContext(), R.string.err_location_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        locationHelper = new LocationHelper(requireContext());

        SessionManager session = new SessionManager(requireContext());
        binding.tvName.setText(session.getFullName());
        binding.tvContact.setText(session.getEmail() + "  •  " + session.getMobile());

        binding.btnAddAddress.setOnClickListener(v -> {
            if (LocationHelper.hasLocationPermission(requireContext())) {
                saveCurrentGpsAddress();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        binding.btnNewComplaint.setOnClickListener(v -> showComplaintDialog());
        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.logoutAndReturnToLogin();
            }
        });

        loadProfileAndData();
    }

    private void loadProfileAndData() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), res -> {
            if (res != null && res.status == Resource.Status.SUCCESS && res.data != null && binding != null) {
                binding.tvName.setText(String.valueOf(res.data.get("fullName")));
                binding.tvContact.setText(res.data.get("email") + "  •  " + res.data.get("mobile"));
            }
        });
        loadAddresses();
        loadComplaints();
    }

    private void loadAddresses() {
        viewModel.getAddresses().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null || res.status != Resource.Status.SUCCESS) return;
            List<AddressModel> list = res.data;
            if (list == null || list.isEmpty()) {
                binding.tvAddressesList.setText("No saved addresses yet.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (AddressModel a : list) {
                sb.append("• ").append(a.label).append(": ").append(a.addressLine).append("\n");
            }
            binding.tvAddressesList.setText(sb.toString().trim());
        });
    }

    private void loadComplaints() {
        viewModel.getMyComplaints().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null || res.status != Resource.Status.SUCCESS) return;
            List<AdminModels.ComplaintItem> list = res.data;
            if (list == null || list.isEmpty()) {
                binding.tvComplaintsList.setText("No complaints raised.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (AdminModels.ComplaintItem c : list) {
                sb.append("• [").append(c.status).append("] ").append(c.subject);
                if (c.adminResponse != null && !c.adminResponse.isEmpty()) {
                    sb.append("\n  Admin: ").append(c.adminResponse);
                }
                sb.append("\n");
            }
            binding.tvComplaintsList.setText(sb.toString().trim());
        });
    }

    private void saveCurrentGpsAddress() {
        locationHelper.fetchCurrentLocation(new LocationHelper.SingleLocationCallback() {
            @Override
            public void onLocation(double lat, double lng, String resolvedAddress) {
                if (!isAdded()) return;
                EditText etLabel = new EditText(requireContext());
                etLabel.setHint("Label (Home / Office)");
                etLabel.setText("Home");
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                etLabel.setPadding(pad, pad, pad, pad);

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Save Address")
                        .setMessage(resolvedAddress)
                        .setView(etLabel)
                        .setPositiveButton("Save", (d, w) -> {
                            String label = etLabel.getText().toString().trim();
                            viewModel.addAddress(label.isEmpty() ? "Home" : label, resolvedAddress, lat, lng)
                                    .observe(getViewLifecycleOwner(), res -> {
                                        if (res != null && res.status == Resource.Status.SUCCESS) {
                                            Toast.makeText(requireContext(), "Address saved!", Toast.LENGTH_SHORT).show();
                                            loadAddresses();
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
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
        etDesc.setHint("Description");
        container.addView(etDesc);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Submit Complaint")
                .setView(container)
                .setPositiveButton("Submit", (d, w) -> {
                    String subj = etSubject.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (subj.isEmpty() || desc.isEmpty()) return;
                    viewModel.submitComplaint(null, subj, desc).observe(getViewLifecycleOwner(), res -> {
                        if (res != null && res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Complaint submitted", Toast.LENGTH_SHORT).show();
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
}
