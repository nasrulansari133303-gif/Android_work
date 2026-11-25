package com.example.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.databinding.FragmentCustomerJobDetailBinding;
import com.example.model.JobModel;
import com.example.utils.Resource;
import com.example.websocket.WebSocketManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class CustomerJobDetailFragment extends Fragment implements OnMapReadyCallback {

    private FragmentCustomerJobDetailBinding binding;
    private CustomerViewModel viewModel;
    private long jobId = -1L;
    private JobModel currentJob;
    private GoogleMap googleMap;
    private Marker customerMarker;
    private Marker workerMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerJobDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        if (getArguments() != null) {
            jobId = getArguments().getLong("jobId", -1L);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.customerJobMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnCallWorker.setOnClickListener(v -> {
            if (currentJob != null && currentJob.workerMobile != null && !currentJob.workerMobile.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentJob.workerMobile)));
            }
        });
        binding.btnPayNow.setOnClickListener(v -> showPaymentDialog());
        binding.btnRateWorker.setOnClickListener(v -> showRatingDialog());
        binding.btnCancelJob.setOnClickListener(v -> showCancelDialog());
        binding.btnRaiseComplaint.setOnClickListener(v -> showComplaintDialog());

        WebSocketManager.getInstance().getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null || event.payload == null) return;
            long eventJobId = event.payload.has("jobId") ? event.payload.get("jobId").getAsLong() : -1L;
            if (eventJobId != jobId) return;

            if ("WORKER_LOCATION_UPDATE".equals(event.type)) {
                double lat = event.payload.get("latitude").getAsDouble();
                double lng = event.payload.get("longitude").getAsDouble();
                updateWorkerMarker(lat, lng);
            } else if ("JOB_ACCEPTED".equals(event.type)
                    || "JOB_STATUS_UPDATED".equals(event.type)
                    || "JOB_CANCELLED".equals(event.type)) {
                loadJobDetail();
            }
        });

        loadJobDetail();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        map.getUiSettings().setZoomControlsEnabled(true);
        if (currentJob != null) {
            renderMapPins(currentJob);
        }
    }

    private void loadJobDetail() {
        if (jobId <= 0) return;
        viewModel.getJobDetail(jobId).observe(getViewLifecycleOwner(), res -> {
            if (res == null || binding == null) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null && res.data.job != null) {
                bindJob(res.data);
            } else if (res.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindJob(JobModel.JobDetailData data) {
        JobModel job = data.job;
        this.currentJob = job;

        binding.tvJobCode.setText(job.jobCode + "  •  " + job.preferredDate + " " + job.preferredTime);
        binding.tvServiceTitle.setText(job.serviceName + ": " + job.problemTitle);
        binding.tvStatusBadge.setText(job.status);

        if (job.workerName != null && !job.workerName.isEmpty()) {
            binding.cardWorker.setVisibility(View.VISIBLE);
            binding.tvWorkerName.setText(job.workerName);
            binding.tvWorkerRating.setText(String.format(Locale.getDefault(),
                    "Rating: %.1f ★  •  %s", job.workerRating, job.workerMobile != null ? job.workerMobile : ""));
        } else {
            binding.cardWorker.setVisibility(View.GONE);
        }

        StringBuilder bill = new StringBuilder();
        bill.append("Address: ").append(job.address).append("\n");
        if (job.description != null && !job.description.isEmpty()) {
            bill.append("Notes: ").append(job.description).append("\n");
        }
        bill.append(String.format(Locale.getDefault(), "Estimated Budget: ₹%.0f\n", job.estimatedBudget));
        if (job.finalAmount > 0) {
            bill.append(String.format(Locale.getDefault(),
                    "Service Charge: ₹%.0f  |  Materials: ₹%.0f\nFinal Bill: ₹%.0f (%s)",
                    job.serviceCharge, job.additionalMaterial, job.finalAmount,
                    job.paymentStatus != null ? job.paymentStatus : "PENDING"));
        }
        if (job.completionNote != null && !job.completionNote.isEmpty()) {
            bill.append("\nWorker Completion Note: ").append(job.completionNote);
        }
        if (job.ratingGiven != null) {
            bill.append("\nYour Rating: ").append(job.ratingGiven).append("/5 ★");
        }
        binding.tvBillSummary.setText(bill.toString());

        StringBuilder timeline = new StringBuilder("Job Timeline\n");
        if (data.statusHistory != null) {
            for (JobModel.StatusHistoryItem item : data.statusHistory) {
                timeline.append("• ").append(item.status).append(" — ").append(item.note).append("\n");
            }
        }
        binding.tvTimeline.setText(timeline.toString().trim());

        boolean canCancel = "PENDING".equals(job.status)
                || "MATCHING".equals(job.status)
                || "ACCEPTED".equals(job.status)
                || "ON_THE_WAY".equals(job.status);
        boolean canPay = "COMPLETED".equals(job.status) && !"PAID".equals(job.paymentStatus);
        boolean canRate = ("PAYMENT".equals(job.status) || "COMPLETED".equals(job.status))
                && job.ratingGiven == null
                && job.workerId != null;

        binding.btnCancelJob.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        binding.btnPayNow.setVisibility(canPay ? View.VISIBLE : View.GONE);
        binding.btnRateWorker.setVisibility(canRate ? View.VISIBLE : View.GONE);

        renderMapPins(job);
    }

    private void renderMapPins(JobModel job) {
        if (googleMap == null) return;
        LatLng custLatLng = new LatLng(job.latitude, job.longitude);
        if (customerMarker == null) {
            customerMarker = googleMap.addMarker(new MarkerOptions().position(custLatLng).title("Service Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(custLatLng, 14f));
        } else {
            customerMarker.setPosition(custLatLng);
        }
        if (job.workerLatitude != null && job.workerLongitude != null) {
            updateWorkerMarker(job.workerLatitude, job.workerLongitude);
        }
    }

    private void updateWorkerMarker(double lat, double lng) {
        if (googleMap == null) return;
        LatLng workerPos = new LatLng(lat, lng);
        if (workerMarker == null) {
            workerMarker = googleMap.addMarker(new MarkerOptions()
                    .position(workerPos)
                    .title("Service Partner")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        } else {
            workerMarker.setPosition(workerPos);
        }
    }

    private void showPaymentDialog() {
        String[] methods = {"Cash After Service", "UPI Payment", "Online Gateway"};
        String[] codes = {"CASH", "UPI", "ONLINE"};
        final int[] chosen = {0};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Payment Method")
                .setSingleChoiceItems(methods, 0, (dialog, which) -> chosen[0] = which)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    String method = codes[chosen[0]];
                    if ("UPI".equals(method)) {
                        promptUpiRefAndPay();
                    } else {
                        executePayment(method, "");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptUpiRefAndPay() {
        EditText input = new EditText(requireContext());
        input.setHint("Enter UPI Transaction ID (optional)");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("UPI Payment Reference")
                .setView(input)
                .setPositiveButton("Confirm UPI Payment", (d, w) ->
                        executePayment("UPI", input.getText().toString().trim()))
                .setNegativeButton("Back", null)
                .show();
    }

    private void executePayment(String method, String ref) {
        viewModel.payJob(jobId, method, ref).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.status == Resource.Status.LOADING) return;
            if (res.status == Resource.Status.SUCCESS && res.data != null) {
                String status = String.valueOf(res.data.get("status"));
                String msg = String.valueOf(res.data.get("message"));
                if ("PAID".equals(status)) {
                    Toast.makeText(requireContext(), "Payment successful!", Toast.LENGTH_LONG).show();
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Payment Status: " + status)
                            .setMessage(msg + "\n\nPlease use Cash or UPI for immediate settlement unless your external gateway webhook is configured.")
                            .setPositiveButton("OK", null)
                            .show();
                }
                loadJobDetail();
            } else {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showRatingDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        RatingBar ratingBar = new RatingBar(requireContext());
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ratingBar.setLayoutParams(lp);
        container.addView(ratingBar);

        EditText etComment = new EditText(requireContext());
        etComment.setHint("Share your feedback about the service...");
        container.addView(etComment);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rate Service Partner")
                .setView(container)
                .setPositiveButton("Submit Rating", (d, w) -> {
                    int stars = Math.max(1, Math.min(5, Math.round(ratingBar.getRating())));
                    viewModel.rateWorker(jobId, stars, etComment.getText().toString().trim())
                            .observe(getViewLifecycleOwner(), res -> {
                                if (res == null || res.status == Resource.Status.LOADING) return;
                                if (res.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Thank you for rating!", Toast.LENGTH_SHORT).show();
                                    loadJobDetail();
                                } else {
                                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showCancelDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Reason for cancellation");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Service Request?")
                .setView(input)
                .setPositiveButton("Cancel Job", (d, w) -> {
                    viewModel.cancelJob(jobId, input.getText().toString().trim())
                            .observe(getViewLifecycleOwner(), res -> {
                                if (res == null || res.status == Resource.Status.LOADING) return;
                                if (res.status == Resource.Status.SUCCESS) {
                                    Toast.makeText(requireContext(), "Job cancelled", Toast.LENGTH_SHORT).show();
                                    loadJobDetail();
                                } else {
                                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Keep Job", null)
                .show();
    }

    private void showComplaintDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etSubject = new EditText(requireContext());
        etSubject.setHint("Issue Subject");
        container.addView(etSubject);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Describe the issue...");
        container.addView(etDesc);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Report an Issue")
                .setView(container)
                .setPositiveButton("Submit", (d, w) -> {
                    String subj = etSubject.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (subj.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(requireContext(), "Subject and description required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.submitComplaint(jobId, subj, desc).observe(getViewLifecycleOwner(), res -> {
                        if (res == null || res.status == Resource.Status.LOADING) return;
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Complaint submitted to Admin", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
