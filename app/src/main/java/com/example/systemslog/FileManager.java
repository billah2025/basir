package com.example.systemslog;

import android.content.Context;
import java.io.*;
import java.util.*;

public class FileManager {

    public static void saveLog(Context context, String text, long timestamp) {
        try {
            String fileName = "keylog_" + timestamp + ".enc";
            String encrypted = EncryptionUtils.encrypt(text);
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(encrypted.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Long, String> getAllLogs(Context context) {
        Map<Long, String> logs = new HashMap<>();
        File[] files = context.getFilesDir().listFiles();

        for (File file : files) {
            if (file.getName().startsWith("keylog_") && file.getName().endsWith(".enc")) {
                try {
                    long timestamp = Long.parseLong(file.getName().split("_")[1].split("\\.")[0]);
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    fis.close();
                    String decrypted = EncryptionUtils.decrypt(new String(data));
                    logs.put(timestamp, decrypted);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return logs;
    }

    public static void deleteAllLogs(Context context) {
        File[] files = context.getFilesDir().listFiles();
        for (File file : files) {
            if (file.getName().startsWith("keylog_")) {
                file.delete();
            }
        }
    }
}
