package com.example.customer;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.R;
import com.example.databinding.FragmentMapPickerBinding;
import com.example.location.LocationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapPickerFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapPickerBinding binding;
    private CustomerViewModel viewModel;
    private LocationHelper locationHelper;
    private GoogleMap googleMap;
    private Marker marker;

    private double selectedLat = 22.3039; // Rajkot default center before GPS fix
    private double selectedLng = 70.8022;
    private String selectedAddress = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (LocationHelper.hasLocationPermission(requireContext())) {
                    moveToCurrentGps();
                } else {
                    Toast.makeText(requireContext(), R.string.err_location_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        locationHelper = new LocationHelper(requireContext());

        Double prevLat = viewModel.getPickedLat().getValue();
        Double prevLng = viewModel.getPickedLng().getValue();
        if (prevLat != null && prevLng != null) {
            selectedLat = prevLat;
            selectedLng = prevLng;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.btnMyGps.setOnClickListener(v -> {
            if (LocationHelper.hasLocationPermission(requireContext())) {
                moveToCurrentGps();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        binding.btnConfirmLocation.setOnClickListener(v -> {
            String addr = selectedAddress.isEmpty()
                    ? locationHelper.reverseGeocode(selectedLat, selectedLng)
                    : selectedAddress;
            viewModel.setPickedLocation(selectedLat, selectedLng, addr);
            Navigation.findNavController(v).popBackStack();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        map.getUiSettings().setZoomControlsEnabled(true);
        updateMarker(new LatLng(selectedLat, selectedLng), true);

        map.setOnMapClickListener(latLng -> updateMarker(latLng, false));

        if (viewModel.getPickedLat().getValue() == null && LocationHelper.hasLocationPermission(requireContext())) {
            moveToCurrentGps();
        }
    }

    private void moveToCurrentGps() {
        locationHelper.fetchCurrentLocation(new LocationHelper.SingleLocationCallback() {
            @Override
            public void onLocation(double lat, double lng, String resolvedAddress) {
                if (binding == null) return;
                selectedAddress = resolvedAddress;
                updateMarker(new LatLng(lat, lng), true);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateMarker(LatLng latLng, boolean zoom) {
        selectedLat = latLng.latitude;
        selectedLng = latLng.longitude;
        if (googleMap != null) {
            if (marker == null) {
                marker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Service Location"));
            } else {
                marker.setPosition(latLng);
            }
            if (zoom) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            }
        }
        binding.tvPickedCoords.setText("Resolving address...");
        executor.execute(() -> {
            String resolved = locationHelper.reverseGeocode(latLng.latitude, latLng.longitude);
            mainHandler.post(() -> {
                selectedAddress = resolved;
                if (binding != null) {
                    binding.tvPickedCoords.setText(resolved);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
        binding = null;
    }
}
