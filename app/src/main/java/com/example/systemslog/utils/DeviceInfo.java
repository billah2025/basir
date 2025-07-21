package com.example.systemslog.utils;

import android.content.Context;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;

public class DeviceInfo {
    private final Context context;

    public DeviceInfo(Context ctx) {
        this.context = ctx;
    }

    public String getName() {
        return Build.MODEL + "_" + Build.SERIAL;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("model", Build.MODEL);
        data.put("brand", Build.BRAND);
        data.put("sdk", Build.VERSION.SDK_INT);
        return data;
    }
}
