package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.example.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class KaamMitraRobolectricTest {

    private Context context;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.clearSession();
    }

    @Test
    public void appNameMatchesPlatformTitle() {
        assertEquals("Kaam-Mitra", context.getString(R.string.app_name));
    }

    @Test
    public void sessionManager_normalizesBaseAndWebSocketUrls() {
        sessionManager.setBaseUrl("192.168.1.45:8080");
        assertEquals("http://192.168.1.45:8080/", sessionManager.getBaseUrl());

        sessionManager.saveAuthSession("jwt-123", 10L, "Rahul", "r@km.in", "9999999999", "CUSTOMER", "VERIFIED");
        assertEquals("ws://192.168.1.45:8080/ws?token=jwt-123", sessionManager.getWebSocketUrl());
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.clearSession();
        assertFalse(sessionManager.isLoggedIn());
        assertEquals("http://192.168.1.45:8080/", sessionManager.getBaseUrl());
    }

    @Test
    public void mainActivity_unauthenticatedHidesBottomNav() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        assertNotNull(bottomNav);
        assertEquals(View.GONE, bottomNav.getVisibility());
    }

    @Test
    public void mainActivity_routesCustomerWorkerAndAdminMenusStrictly() {
        sessionManager.saveAuthSession("tok-cust", 1L, "Cust", "c@km.in", "9800000001", "CUSTOMER", "VERIFIED");
        MainActivity custActivity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        BottomNavigationView custNav = custActivity.findViewById(R.id.bottomNavigation);
        assertEquals(View.VISIBLE, custNav.getVisibility());
        assertNotNull(custNav.getMenu().findItem(R.id.customerHomeFragment));
        assertEquals(null, custNav.getMenu().findItem(R.id.adminDashboardFragment));

        sessionManager.saveAuthSession("tok-work", 2L, "Work", "w@km.in", "9800000002", "WORKER", "VERIFIED");
        MainActivity workerActivity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        BottomNavigationView workerNav = workerActivity.findViewById(R.id.bottomNavigation);
        assertEquals(View.VISIBLE, workerNav.getVisibility());
        assertNotNull(workerNav.getMenu().findItem(R.id.workerHomeFragment));
        assertEquals(null, workerNav.getMenu().findItem(R.id.adminDashboardFragment));

        sessionManager.saveAuthSession("tok-adm", 3L, "Admin", "a@km.in", "9800000003", "ADMIN", "VERIFIED");
        MainActivity adminActivity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        BottomNavigationView adminNav = adminActivity.findViewById(R.id.bottomNavigation);
        assertEquals(View.VISIBLE, adminNav.getVisibility());
        assertNotNull(adminNav.getMenu().findItem(R.id.adminDashboardFragment));
    }
}
