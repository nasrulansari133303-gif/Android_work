package com.example.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface SingleLocationCallback {
        void onLocation(double lat, double lng, String resolvedAddress);
        void onError(String message);
    }

    public interface TrackingCallback {
        void onLocationUpdate(double lat, double lng);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private LocationCallback trackingCallback;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGpsEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    public void fetchCurrentLocation(SingleLocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onError("Location permission is required.");
            return;
        }
        if (!isGpsEnabled(context)) {
            callback.onError("GPS is disabled. Please turn on device location.");
            return;
        }
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocation(location.getLatitude(), location.getLongitude(),
                        reverseGeocode(location.getLatitude(), location.getLongitude()));
            } else {
                requestSingleFreshFix(callback);
            }
        }).addOnFailureListener(e -> requestSingleFreshFix(callback));
    }

    @SuppressLint("MissingPermission")
    private void requestSingleFreshFix(SingleLocationCallback callback) {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMaxUpdates(1)
                .build();
        fusedClient.requestLocationUpdates(req, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    callback.onLocation(loc.getLatitude(), loc.getLongitude(),
                            reverseGeocode(loc.getLatitude(), loc.getLongitude()));
                } else {
                    callback.onError("Unable to obtain GPS fix. Please select location on map.");
                }
            }
        }, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    public void startPeriodicTracking(long intervalMs, TrackingCallback callback) {
        if (!hasLocationPermission(context) || !isGpsEnabled(context)) return;
        stopTracking();
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(15f)
                .build();
        trackingCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    callback.onLocationUpdate(loc.getLatitude(), loc.getLongitude());
                }
            }
        };
        fusedClient.requestLocationUpdates(req, trackingCallback, Looper.getMainLooper());
    }

    public void stopTracking() {
        if (trackingCallback != null) {
            fusedClient.removeLocationUpdates(trackingCallback);
            trackingCallback = null;
        }
    }

    public String reverseGeocode(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String line = addresses.get(0).getAddressLine(0);
                if (line != null && !line.trim().isEmpty()) return line;
            }
        } catch (Exception ignored) {}
        return String.format(Locale.US, "Lat %.4f, Lng %.4f", lat, lng);
    }
}
