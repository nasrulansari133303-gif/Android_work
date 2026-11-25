package com.example.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

import com.example.MainActivity;
import com.example.R;

public final class NotificationHelper {

    public static final String CHANNEL_JOBS = "kaam_mitra_jobs";
    public static final String CHANNEL_GENERAL = "kaam_mitra_general";

    private NotificationHelper() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel jobsChannel = new NotificationChannel(
                    CHANNEL_JOBS,
                    "Service & Job Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            jobsChannel.setDescription("Real-time incoming service requests and job status updates");
            jobsChannel.enableVibration(true);

            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Updates",
                    NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("Account verification, payments, and announcements");

            nm.createNotificationChannel(jobsChannel);
            nm.createNotificationChannel(generalChannel);
        }
    }

    public static void showNotification(Context context, String title, String body, boolean highPriority) {
        createChannels(context);
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, highPriority ? CHANNEL_JOBS : CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.ic_service_wrench)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(highPriority ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) (System.currentTimeMillis() & 0x7FFFFFFF), builder.build());
        }
    }

    public static void triggerAlertVibrationAndSound(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vm.getDefaultVibrator().vibrate(
                            VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(450);
                    }
                }
            }
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            var ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {}
    }
}
