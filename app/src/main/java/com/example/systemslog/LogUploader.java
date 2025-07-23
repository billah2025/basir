package com.example.systemslog;

import android.content.Context;
import android.util.Log;
import androidx.work.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LogUploader extends Worker {

    public LogUploader(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Map<Long, String> logs = FileManager.getAllLogs(context);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String device = android.os.Build.MODEL.replace(" ", "_");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (Map.Entry<Long, String> entry : logs.entrySet()) {
            String date = dateFormat.format(new Date(entry.getKey()));
            String time = timeFormat.format(new Date(entry.getKey()));
            String path = String.format("devices/%s/%s/%s", device, date, time);

            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("log", entry.getValue());

            db.document(path)
                    .set(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d("LogUploader", "✅ Uploaded log at " + path))
                    .addOnFailureListener(e -> Log.e("LogUploader", "❌ Failed upload", e));
        }

        FileManager.deleteAllLogs(context); // clear local logs after sending
        return Result.success();
    }

    public static void scheduleUploader(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                LogUploader.class, 20, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "LogUploaderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
