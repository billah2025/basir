package com.example.systemslog;

import android.content.Context;
import android.util.Log;
import androidx.work.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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

        if (logs.isEmpty()) {
            return Result.success(); // Nothing to upload
        }

        final CountDownLatch latch = new CountDownLatch(logs.size());
        final List<Boolean> successList = Collections.synchronizedList(new ArrayList<>());

        for (Map.Entry<Long, String> entry : logs.entrySet()) {
            String date = dateFormat.format(new Date(entry.getKey()));
            String time = timeFormat.format(new Date(entry.getKey()));
            String path = String.format("devices/%s/%s/%s", device, date, time);

            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("log", entry.getValue());

            db.document(path)
                    .set(logEntry)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("LogUploader", "✅ Uploaded log at " + path);
                        successList.add(true);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LogUploader", "❌ Failed upload", e);
                        successList.add(false);
                        latch.countDown();
                    });
        }

        try {
            latch.await(30, TimeUnit.SECONDS); // wait for all uploads to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.retry();
        }

        if (successList.contains(false)) {
            return Result.retry(); // Retry later if any upload failed
        } else {
            FileManager.deleteAllLogs(context); // delete only if all uploads succeeded
            return Result.success();
        }
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
