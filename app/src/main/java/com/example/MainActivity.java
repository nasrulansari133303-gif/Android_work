package com.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.databinding.ActivityMainBinding;
import com.example.notification.NotificationHelper;
import com.example.utils.SessionManager;
import com.example.websocket.WebSocketManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private SessionManager sessionManager;
    private int lastNavGraphRes = 0;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        NotificationHelper.createChannels(this);
        requestNotificationPermissionIfNeeded();

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean navVisible = binding.bottomNavigation.getVisibility() == View.VISIBLE;
            binding.navHostFragment.setPadding(
                    bars.left,
                    bars.top,
                    bars.right,
                    navVisible ? 0 : bars.bottom
            );
            binding.bottomNavigation.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean hideBottomNav = !sessionManager.isLoggedIn()
                        || id == R.id.loginFragment
                        || id == R.id.registerCustomerFragment
                        || id == R.id.registerWorkerFragment
                        || id == R.id.createJobFragment
                        || id == R.id.mapPickerFragment
                        || id == R.id.customerJobDetailFragment
                        || id == R.id.workerActiveJobFragment;
                binding.bottomNavigation.setVisibility(hideBottomNav ? View.GONE : View.VISIBLE);
                ViewCompat.requestApplyInsets(binding.mainRoot);
            });
        }

        routeUserByRole();
    }

    public void routeUserByRole() {
        if (navController == null) return;

        if (!sessionManager.isLoggedIn()) {
            WebSocketManager.getInstance().disconnect();
            binding.bottomNavigation.setVisibility(View.GONE);
            binding.bottomNavigation.getMenu().clear();
            if (lastNavGraphRes != R.navigation.nav_auth) {
                lastNavGraphRes = R.navigation.nav_auth;
                navController.setGraph(R.navigation.nav_auth);
            }
            ViewCompat.requestApplyInsets(binding.mainRoot);
            return;
        }

        String role = sessionManager.getRole();
        int targetGraph;
        int targetMenu;
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            targetGraph = R.navigation.nav_customer;
            targetMenu = R.menu.menu_customer_bottom;
        } else if ("WORKER".equalsIgnoreCase(role)) {
            targetGraph = R.navigation.nav_worker;
            targetMenu = R.menu.menu_worker_bottom;
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            targetGraph = R.navigation.nav_admin;
            targetMenu = R.menu.menu_admin_bottom;
        } else {
            sessionManager.clearSession();
            routeUserByRole();
            return;
        }

        binding.bottomNavigation.getMenu().clear();
        binding.bottomNavigation.inflateMenu(targetMenu);
        binding.bottomNavigation.setVisibility(View.VISIBLE);

        lastNavGraphRes = targetGraph;
        navController.setGraph(targetGraph);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        WebSocketManager.getInstance().connect(this);
        ViewCompat.requestApplyInsets(binding.mainRoot);
    }

    public void logoutAndReturnToLogin() {
        sessionManager.clearSession();
        WebSocketManager.getInstance().disconnect();
        lastNavGraphRes = 0;
        routeUserByRole();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            WebSocketManager.getInstance().disconnect();
        }
    }
}
