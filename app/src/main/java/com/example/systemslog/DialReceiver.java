package com.example.systemslog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class DialReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        // Secret code to trigger the hidden activity
        if ("*#12345#".equals(number)) {
            setResultData(null); // Cancel actual dial

            Intent i = new Intent(context, StealthActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

            Toast.makeText(context, "Opening hidden app...", Toast.LENGTH_SHORT).show();
        }
    }
}
