package com.example.worker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.FragmentWorkerActiveJobBinding;
import com.example.location.LocationHelper;
import com.example.model.JobModel;
import com.example.utils.Resource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class WorkerActiveJobFragment extends Fragment implements OnMapReadyCallback {

    private FragmentWorkerActiveJobBinding binding;
    private WorkerViewModel viewModel;
    private LocationHelper locationHelper;
    private GoogleMap googleMap;
    private long jobId = -1L;
    private JobModel currentJob;
    private String uploadedCompletionPhotoUrl = "";
    private TextView tvCompletionPhotoStatus;

    private final ActivityResultLauncher<PickVisualMediaRequest> completionPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    uploadCompletionPhoto(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerActiveJobBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkerViewModel.class);
        locationHelper = new LocationHelper(requireContext());
        if (getArguments() != null) {
            jobId = getArguments().getLong("jobId", -1L);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.workerJobMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnCallCustomer.setOnClickListener(v -> {
            if (currentJob != null && currentJob.customerMobile != null && !currentJob.customerMobile.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentJob.customerMobile)));
            }
        });

        binding.btnNavigate.setOnClickListener(v -> launchNavigation());
        binding.btnPrimaryAction.setOnClickListener(v -> performNextStep());

        if (LocationHelper.hasLocationPermission(requireContext())) {
            locationHelper.startPeriodicTracking(15000L, (lat, lng) -> viewModel.pushLocation(lat, lng));
        }

        loadJob();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        map.getUiSettings().setZoomControlsEnabled(true);
        if (currentJob != null) {
            renderCustomerPin(currentJob);
        }
    }

    private void loadJob() {
        viewModel.getWorkerJobs().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null && res.data.myJobs != null) {
                for (JobModel job : res.data.myJobs) {
                    if (job.id == jobId) {
                        bindJob(job);
                        break;
                    }
                }
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindJob(JobModel job) {
        this.currentJob = job;
        binding.tvJobCodeStatus.setText(job.jobCode + "  •  " + job.status);
        binding.tvServiceName.setText(job.serviceName);
        binding.tvCustomerName.setText("Customer: " + job.customerName);
        binding.tvLocationAddress.setText(job.address);

        double baseOrFinal = job.finalAmount > 0 ? job.finalAmount : job.estimatedBudget;
        binding.tvDistanceEarning.setText(String.format(Locale.getDefault(),
                "Budget / Bill: ₹%.0f  •  Payment: %s",
                baseOrFinal, job.paymentStatus != null ? job.paymentStatus : "PENDING"));

        binding.tvProblemDetails.setText("Problem: " + job.problemTitle
                + (job.description != null && !job.description.isEmpty() ? "\n" + job.description : "")
                + "\nPreferred Slot: " + job.preferredDate + " " + job.preferredTime);

        binding.btnPrimaryAction.setEnabled(true);
        switch (job.status) {
            case "ACCEPTED" -> binding.btnPrimaryAction.setText("START TRAVEL (ON THE WAY)");
            case "ON_THE_WAY" -> binding.btnPrimaryAction.setText(R.string.i_have_arrived);
            case "ARRIVED" -> binding.btnPrimaryAction.setText(R.string.start_work);
            case "IN_PROGRESS" -> binding.btnPrimaryAction.setText(R.string.complete_job);
            default -> {
                binding.btnPrimaryAction.setText("JOB " + job.status);
                binding.btnPrimaryAction.setEnabled(false);
            }
        }
        renderCustomerPin(job);
    }

    private void renderCustomerPin(JobModel job) {
        if (googleMap == null) return;
        LatLng pos = new LatLng(job.latitude, job.longitude);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(pos).title(job.customerName + " (" + job.serviceName + ")"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }

    private void launchNavigation() {
        if (currentJob == null) return;
        Uri navUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f",
                currentJob.latitude, currentJob.longitude));
        Intent intent = new Intent(Intent.ACTION_VIEW, navUri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Uri geoUri = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                    currentJob.latitude, currentJob.longitude,
                    currentJob.latitude, currentJob.longitude,
                    Uri.encode(currentJob.address)));
            startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
        }
    }

    private void performNextStep() {
        if (currentJob == null) return;
        switch (currentJob.status) {
            case "ACCEPTED" -> observeTransition(viewModel.markOnTheWay(jobId));
            case "ON_THE_WAY" -> observeTransition(viewModel.markArrived(jobId));
            case "ARRIVED" -> observeTransition(viewModel.startWork(jobId));
            case "IN_PROGRESS" -> showCompletionBillDialog();
            default -> {}
        }
    }

    private void observeTransition(androidx.lifecycle.LiveData<Resource<JobModel>> live) {
        binding.btnPrimaryAction.setEnabled(false);
        live.observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING || binding == null) return;
            binding.btnPrimaryAction.setEnabled(true);
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                bindJob(res.data);
            } else {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCompletionBillDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etServiceCharge = new EditText(requireContext());
        etServiceCharge.setHint("Service Charge (₹)");
        etServiceCharge.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etServiceCharge.setText(String.format(Locale.US, "%.0f", currentJob.estimatedBudget));
        container.addView(etServiceCharge);

        EditText etMaterial = new EditText(requireContext());
        etMaterial.setHint("Additional Material Cost (₹)");
        etMaterial.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etMaterial.setText("0");
        container.addView(etMaterial);

        TextView tvTotal = new TextView(requireContext());
        tvTotal.setTextSize(16f);
        tvTotal.setPadding(0, pad / 2, 0, pad / 2);
        container.addView(tvTotal);

        Runnable recalc = () -> {
            double sc = parseDouble(etServiceCharge.getText().toString());
            double mat = parseDouble(etMaterial.getText().toString());
            tvTotal.setText(String.format(Locale.getDefault(), "Total Customer Bill: ₹%.0f", sc + mat));
        };
        recalc.run();

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { recalc.run(); }
        };
        etServiceCharge.addTextChangedListener(watcher);
        etMaterial.addTextChangedListener(watcher);

        EditText etNote = new EditText(requireContext());
        etNote.setHint("Completion Work Summary Note");
        container.addView(etNote);

        Button btnPhoto = new Button(requireContext());
        btnPhoto.setText("Attach Completion Photo (Optional)");
        btnPhoto.setOnClickListener(v -> completionPhotoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        container.addView(btnPhoto);

        tvCompletionPhotoStatus = new TextView(requireContext());
        tvCompletionPhotoStatus.setText(uploadedCompletionPhotoUrl.isEmpty() ? "No photo attached" : "Photo attached");
        container.addView(tvCompletionPhotoStatus);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Complete Job & Generate Bill")
                .setView(container)
                .setPositiveButton("Submit & Complete", (d, w) -> {
                    double sc = parseDouble(etServiceCharge.getText().toString());
                    double mat = parseDouble(etMaterial.getText().toString());
                    if (sc <= 0) {
                        Toast.makeText(requireContext(), "Service charge must be > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JobModel.CompleteJobRequest req = new JobModel.CompleteJobRequest(
                            sc, mat, etNote.getText().toString().trim(), uploadedCompletionPhotoUrl);
                    observeTransition(viewModel.completeJob(jobId, req));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadCompletionPhoto(Uri uri) {
        if (tvCompletionPhotoStatus != null) tvCompletionPhotoStatus.setText("Uploading photo...");
        viewModel.uploadPhoto(uri, "completions").observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                uploadedCompletionPhotoUrl = res.data;
                if (tvCompletionPhotoStatus != null) tvCompletionPhotoStatus.setText("Completion photo attached");
            } else {
                if (tvCompletionPhotoStatus != null) tvCompletionPhotoStatus.setText("Upload failed: " + res.message);
            }
        });
    }

    private double parseDouble(String s) {
        try {
            return s == null || s.trim().isEmpty() ? 0.0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationHelper.stopTracking();
        binding = null;
    }
}
