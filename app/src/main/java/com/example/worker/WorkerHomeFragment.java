package com.example.worker;

import android.Manifest;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.databinding.DialogIncomingJobRequestBinding;
import com.example.databinding.FragmentWorkerHomeBinding;
import com.example.location.LocationHelper;
import com.example.model.JobModel;
import com.example.model.WorkerModels;
import com.example.notification.NotificationHelper;
import com.example.utils.Resource;
import com.example.utils.SessionManager;
import com.example.websocket.WebSocketManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

public class WorkerHomeFragment extends Fragment {

    private FragmentWorkerHomeBinding binding;
    private WorkerViewModel viewModel;
    private LocationHelper locationHelper;
    private WorkerRequestAdapter requestAdapter;

    private boolean suppressSwitchListener = false;
    private boolean currentOnline = false;
    private Long activeJobId = null;
    private AlertDialog incomingDialog;
    private CountDownTimer incomingTimer;
    private long lastIncomingDialogTimestamp = 0L;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationHelper.hasLocationPermission(requireContext())) {
                    goOnlineWithGps();
                } else {
                    setSwitchCheckedSafely(false);
                    Toast.makeText(requireContext(), R.string.err_location_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkerViewModel.class);
        locationHelper = new LocationHelper(requireContext());

        SessionManager session = new SessionManager(requireContext());
        binding.tvGreeting.setText("Welcome, " + (session.getFullName().isEmpty() ? "Partner" : session.getFullName()));

        requestAdapter = new WorkerRequestAdapter(new WorkerRequestAdapter.RequestActionListener() {
            @Override
            public void onAccept(JobModel job) {
                acceptJob(job.id);
            }

            @Override
            public void onReject(JobModel job) {
                rejectJob(job.id);
            }
        });
        binding.rvAvailableRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAvailableRequests.setAdapter(requestAdapter);

        binding.switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchListener) return;
            if (isChecked) {
                if (LocationHelper.hasLocationPermission(requireContext())) {
                    goOnlineWithGps();
                } else {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
            } else {
                goOffline();
            }
        });

        binding.btnOpenActiveJob.setOnClickListener(v -> openActiveJob());
        binding.swipeRefresh.setOnRefreshListener(this::loadDashboard);

        WebSocketManager.getInstance().getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null || binding == null) return;
            if ("NEW_JOB_REQUEST".equals(event.type) && event.timestamp > lastIncomingDialogTimestamp) {
                lastIncomingDialogTimestamp = event.timestamp;
                showIncomingJobDialog(event.payload);
                loadJobs();
            } else if ("JOB_CANCELLED".equals(event.type) || "JOB_STATUS_UPDATED".equals(event.type)) {
                loadDashboard();
            }
        });

        loadDashboard();
    }

    private void loadDashboard() {
        loadProfile();
        loadEarningsSummary();
        loadJobs();
    }

    private void loadProfile() {
        viewModel.getProfile().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                WorkerModels.WorkerProfile p = res.data;
                new SessionManager(requireContext()).setVerificationStatus(p.verificationStatus);
                binding.tvCompletedCount.setText(String.valueOf(p.completedJobsCount));
                binding.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", p.averageRating));

                if (!"VERIFIED".equalsIgnoreCase(p.verificationStatus)) {
                    binding.tvVerificationBanner.setVisibility(View.VISIBLE);
                    String reason = p.rejectionReason != null && !p.rejectionReason.isEmpty()
                            ? " (" + p.rejectionReason + ")" : "";
                    binding.tvVerificationBanner.setText("Account status: " + p.verificationStatus
                            + reason + ". Admin KYC verification required before going ONLINE.");
                    binding.switchOnline.setEnabled(false);
                } else {
                    binding.tvVerificationBanner.setVisibility(View.GONE);
                    binding.switchOnline.setEnabled(true);
                }

                updateOnlineUi(p.online);
                if (p.online && LocationHelper.hasLocationPermission(requireContext())) {
                    locationHelper.startPeriodicTracking(15000L, (lat, lng) -> viewModel.pushLocation(lat, lng));
                } else {
                    locationHelper.stopTracking();
                }
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEarningsSummary() {
        viewModel.getEarnings().observe(getViewLifecycleOwner(), res -> {
            if (res != null && res.status == Resource.Status.SUCCESS && res.data != null && binding != null) {
                binding.tvTodayEarnings.setText(String.format(Locale.getDefault(), "₹%.0f", res.data.todayEarnings));
            }
        });
    }

    private void loadJobs() {
        viewModel.getWorkerJobs().observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                binding.swipeRefresh.setRefreshing(false);
                List<JobModel> available = res.data.availableRequests;
                requestAdapter.submitList(available);
                binding.tvNoRequests.setVisibility(
                        (available == null || available.isEmpty()) ? View.VISIBLE : View.GONE);

                JobModel active = null;
                if (res.data.myJobs != null) {
                    for (JobModel job : res.data.myJobs) {
                        if ("ACCEPTED".equals(job.status)
                                || "ON_THE_WAY".equals(job.status)
                                || "ARRIVED".equals(job.status)
                                || "IN_PROGRESS".equals(job.status)) {
                            active = job;
                            break;
                        }
                    }
                }
                if (active != null) {
                    activeJobId = active.id;
                    binding.cardActiveJob.setVisibility(View.VISIBLE);
                    binding.tvActiveJobTitle.setText(active.jobCode + " • " + active.serviceName + " (" + active.status + ")");
                } else {
                    activeJobId = null;
                    binding.cardActiveJob.setVisibility(View.GONE);
                }
            } else if (res.status == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void goOnlineWithGps() {
        locationHelper.fetchCurrentLocation(new LocationHelper.SingleLocationCallback() {
            @Override
            public void onLocation(double lat, double lng, String resolvedAddress) {
                if (!isAdded()) return;
                viewModel.setOnlineStatus(true, lat, lng).observe(getViewLifecycleOwner(), res -> {
                    if (res == null || res.status == Resource.Status.LOADING || binding == null) return;
                    if (res.status == Resource.Status.SUCCESS) {
                        updateOnlineUi(true);
                        locationHelper.startPeriodicTracking(15000L, (uLat, uLng) -> viewModel.pushLocation(uLat, uLng));
                    } else {
                        setSwitchCheckedSafely(false);
                        Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                setSwitchCheckedSafely(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goOffline() {
        viewModel.setOnlineStatus(false, null, null).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING || binding == null) return;
            if (res.status == Resource.Status.SUCCESS) {
                updateOnlineUi(false);
                locationHelper.stopTracking();
            } else {
                setSwitchCheckedSafely(currentOnline);
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateOnlineUi(boolean online) {
        currentOnline = online;
        setSwitchCheckedSafely(online);
        if (online) {
            binding.cardOnlineStatus.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.km_teal_soft));
            binding.tvStatusLabel.setText("🟢 " + getString(R.string.worker_online));
            binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.km_teal));
            binding.tvStatusSubtitle.setText(R.string.worker_online_desc);
        } else {
            binding.cardOnlineStatus.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.km_error_soft));
            binding.tvStatusLabel.setText("🔴 " + getString(R.string.worker_offline));
            binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.km_error));
            binding.tvStatusSubtitle.setText(R.string.worker_offline_desc);
        }
    }

    private void setSwitchCheckedSafely(boolean checked) {
        suppressSwitchListener = true;
        binding.switchOnline.setChecked(checked);
        suppressSwitchListener = false;
    }

    private void showIncomingJobDialog(JsonObject payload) {
        if (!isAdded() || payload == null) return;
        dismissIncomingDialog();
        NotificationHelper.triggerAlertVibrationAndSound(requireContext());

        long jobId = payload.has("jobId") ? payload.get("jobId").getAsLong() : -1L;
        String serviceName = payload.has("serviceName") ? payload.get("serviceName").getAsString() : "Service Request";
        String customerName = payload.has("customerName") ? payload.get("customerName").getAsString() : "Customer";
        String problem = payload.has("problemTitle") ? payload.get("problemTitle").getAsString() : "";
        String address = payload.has("address") ? payload.get("address").getAsString() : "";
        double dist = payload.has("distanceKm") ? payload.get("distanceKm").getAsDouble() : 0.0;
        double est = payload.has("estimatedEarning") ? payload.get("estimatedEarning").getAsDouble() : 0.0;
        int ttl = payload.has("expiresInSeconds") ? payload.get("expiresInSeconds").getAsInt() : 30;
        int displaySeconds = Math.min(30, Math.max(10, ttl));

        DialogIncomingJobRequestBinding dlgBinding = DialogIncomingJobRequestBinding.inflate(getLayoutInflater());
        dlgBinding.tvReqService.setText(serviceName);
        dlgBinding.tvReqCustomer.setText("Customer: " + customerName);
        dlgBinding.tvReqDistanceAndEarning.setText(String.format(Locale.getDefault(),
                "Distance: %.1f km  •  Est. Earning: ₹%.0f", dist, est));
        dlgBinding.tvReqProblem.setText(problem + "\n" + address);
        dlgBinding.pbCountdown.setMax(displaySeconds);
        dlgBinding.pbCountdown.setProgress(displaySeconds);

        incomingDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dlgBinding.getRoot())
                .setCancelable(false)
                .create();

        dlgBinding.btnDialogAccept.setOnClickListener(v -> {
            dismissIncomingDialog();
            if (jobId > 0) acceptJob(jobId);
        });
        dlgBinding.btnDialogReject.setOnClickListener(v -> {
            dismissIncomingDialog();
            if (jobId > 0) rejectJob(jobId);
        });

        incomingTimer = new CountDownTimer(displaySeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) (millisUntilFinished / 1000L);
                dlgBinding.tvCountdown.setText("Request expires in " + sec + "s");
                dlgBinding.pbCountdown.setProgress(sec);
            }

            @Override
            public void onFinish() {
                dismissIncomingDialog();
                loadJobs();
            }
        };
        incomingDialog.show();
        incomingTimer.start();
    }

    private void dismissIncomingDialog() {
        if (incomingTimer != null) {
            incomingTimer.cancel();
            incomingTimer = null;
        }
        if (incomingDialog != null && incomingDialog.isShowing()) {
            incomingDialog.dismiss();
            incomingDialog = null;
        }
    }

    private void acceptJob(long jobId) {
        viewModel.acceptJob(jobId).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                activeJobId = res.data.id;
                openActiveJob();
            } else {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                loadJobs();
            }
        });
    }

    private void rejectJob(long jobId) {
        viewModel.rejectJob(jobId).observe(getViewLifecycleOwner(), res -> {
            if (res != null && res.status != Resource.Status.LOADING) {
                loadJobs();
            }
        });
    }

    private void openActiveJob() {
        if (activeJobId == null || getView() == null) return;
        Bundle args = new Bundle();
        args.putLong("jobId", activeJobId);
        Navigation.findNavController(getView()).navigate(R.id.action_workerHome_to_activeJob, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissIncomingDialog();
        locationHelper.stopTracking();
        binding = null;
    }
}
