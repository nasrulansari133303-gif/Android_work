package com.example.customer;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.R;
import com.example.databinding.FragmentCreateJobBinding;
import com.example.location.LocationHelper;
import com.example.model.AddressModel;
import com.example.model.JobModel;
import com.example.model.ServiceModel;
import com.example.utils.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateJobFragment extends Fragment {

    private FragmentCreateJobBinding binding;
    private CustomerViewModel viewModel;
    private LocationHelper locationHelper;

    private ServiceModel currentService;
    private Double selectedLat = null;
    private Double selectedLng = null;
    private Uri pendingCameraUri = null;
    private final List<String> uploadedPhotoUrls = new ArrayList<>();

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationHelper.hasLocationPermission(requireContext())) {
                    fetchGpsLocation();
                } else {
                    showError(getString(R.string.err_location_denied));
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
                    uploadProblemPhoto(pendingCameraUri);
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (uris != null) {
                    for (Uri uri : uris) {
                        uploadProblemPhoto(uri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateJobBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        locationHelper = new LocationHelper(requireContext());

        viewModel.getSelectedService().observe(getViewLifecycleOwner(), service -> {
            if (service != null) {
                currentService = service;
                binding.tvSelectedService.setText(service.name);
                if (TextUtils.isEmpty(binding.etBudget.getText())) {
                    binding.etBudget.setText(String.format(Locale.US, "%.0f", service.basePrice));
                }
            }
        });

        viewModel.getPickedLat().observe(getViewLifecycleOwner(), lat -> selectedLat = lat);
        viewModel.getPickedLng().observe(getViewLifecycleOwner(), lng -> selectedLng = lng);
        viewModel.getPickedAddress().observe(getViewLifecycleOwner(), addr -> {
            if (addr != null && !addr.isEmpty()) {
                binding.etAddress.setText(addr);
            }
        });

        binding.btnCamera.setOnClickListener(v -> launchCamera());
        binding.btnGallery.setOnClickListener(v ->
                galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.btnCurrentLocation.setOnClickListener(v -> {
            if (LocationHelper.hasLocationPermission(requireContext())) {
                fetchGpsLocation();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        binding.btnPickOnMap.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_createJob_to_mapPicker));

        binding.btnSavedAddresses.setOnClickListener(v -> showSavedAddressesDialog());

        binding.etPreferredDate.setFocusable(false);
        binding.etPreferredDate.setOnClickListener(v -> showDatePicker());
        binding.etPreferredTime.setFocusable(false);
        binding.etPreferredTime.setOnClickListener(v -> showTimePicker());

        binding.btnRequestService.setOnClickListener(v -> submitJob(v));
    }

    private void launchCamera() {
        try {
            File imagesDir = new File(requireContext().getCacheDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();
            File photoFile = new File(imagesDir, "job_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            showError("Could not launch camera: " + e.getMessage());
        }
    }

    private void uploadProblemPhoto(Uri uri) {
        binding.tvPhotosCount.setText(uploadedPhotoUrls.size() + " photo(s) attached (uploading...)");
        viewModel.uploadPhoto(uri).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING || binding == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                uploadedPhotoUrls.add(res.data);
                binding.tvPhotosCount.setText(uploadedPhotoUrls.size() + " photo(s) attached");
            } else {
                binding.tvPhotosCount.setText(uploadedPhotoUrls.size() + " photo(s) attached (upload failed)");
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchGpsLocation() {
        binding.btnCurrentLocation.setEnabled(false);
        binding.btnCurrentLocation.setText("Locating...");
        locationHelper.fetchCurrentLocation(new LocationHelper.SingleLocationCallback() {
            @Override
            public void onLocation(double lat, double lng, String resolvedAddress) {
                if (binding == null) return;
                binding.btnCurrentLocation.setEnabled(true);
                binding.btnCurrentLocation.setText("Use GPS");
                selectedLat = lat;
                selectedLng = lng;
                viewModel.setPickedLocation(lat, lng, resolvedAddress);
                binding.etAddress.setText(resolvedAddress);
                binding.tvError.setVisibility(View.GONE);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.btnCurrentLocation.setEnabled(true);
                binding.btnCurrentLocation.setText("Use GPS");
                showError(message);
            }
        });
    }

    private void showSavedAddressesDialog() {
        viewModel.getAddresses().observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null && !res.data.isEmpty()) {
                List<AddressModel> list = res.data;
                String[] labels = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    AddressModel a = list.get(i);
                    labels[i] = a.label + " — " + a.addressLine;
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Saved Addresses")
                        .setItems(labels, (dialog, which) -> {
                            AddressModel chosen = list.get(which);
                            selectedLat = chosen.latitude;
                            selectedLng = chosen.longitude;
                            viewModel.setPickedLocation(chosen.latitude, chosen.longitude, chosen.addressLine);
                            binding.etAddress.setText(chosen.addressLine);
                        })
                        .show();
            } else {
                Toast.makeText(requireContext(), "No saved addresses yet. Add one in Profile or use GPS.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(y, m, d);
            String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(chosen.getTime());
            binding.etPreferredDate.setText(formatted);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000L);
        dlg.show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
            chosen.set(Calendar.MINUTE, minute);
            String formatted = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(chosen.getTime());
            binding.etPreferredTime.setText(formatted);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void submitJob(View navView) {
        if (currentService == null) {
            showError("No service selected. Please return and choose a service.");
            return;
        }
        String problem = textOf(binding.etProblemTitle);
        String desc = textOf(binding.etDescription);
        String address = textOf(binding.etAddress);
        String date = textOf(binding.etPreferredDate);
        String time = textOf(binding.etPreferredTime);
        String budgetStr = textOf(binding.etBudget);

        if (TextUtils.isEmpty(problem)) {
            showError("Please enter the problem title.");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            showError("Please enter or pick your service address.");
            return;
        }
        if (selectedLat == null || selectedLng == null) {
            showError("Please tap 'Use GPS' or 'Pick on Map' so we can match the nearest worker.");
            return;
        }

        double budget = currentService.basePrice;
        try {
            if (!budgetStr.isEmpty()) budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            showError("Enter a valid numeric budget.");
            return;
        }

        binding.tvError.setVisibility(View.GONE);
        JobModel.CreateJobRequest req = new JobModel.CreateJobRequest();
        req.serviceId = currentService.id;
        req.problemTitle = problem;
        req.description = desc;
        req.address = address;
        req.latitude = selectedLat;
        req.longitude = selectedLng;
        req.preferredDate = date.isEmpty() ? "Today" : date;
        req.preferredTime = time.isEmpty() ? "ASAP" : time;
        req.estimatedBudget = budget;
        req.photoUrls = new ArrayList<>(uploadedPhotoUrls);

        viewModel.createJob(req).observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnRequestService.setEnabled(false);
            } else if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRequestService.setEnabled(true);
                Toast.makeText(requireContext(),
                        "Job " + res.data.jobCode + " created! Matching nearby workers...",
                        Toast.LENGTH_LONG).show();
                Bundle args = new Bundle();
                args.putLong("jobId", res.data.id);
                Navigation.findNavController(navView).navigate(R.id.action_createJob_to_jobDetail, args);
            } else if (res.status == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRequestService.setEnabled(true);
                showError(res.message);
            }
        });
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
