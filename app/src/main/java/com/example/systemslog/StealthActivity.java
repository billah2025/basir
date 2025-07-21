package com.example.systemslog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StealthActivity extends Activity {

    private Handler handler = new Handler();
    private Runnable autoCloseRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make screen content secure (no screenshots/screen recording)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        // Create a simple UI programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(60, 100, 60, 100);

        TextView textView = new TextView(this);
        textView.setText("🔐 Hidden Logger Panel\n\nThis window will close in 30 seconds...");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        layout.addView(textView);

        // Add button to open MainActivity
        Button openMainBtn = new Button(this);
        openMainBtn.setText("Open Main Settings");
        openMainBtn.setBackgroundColor(Color.DKGRAY);
        openMainBtn.setTextColor(Color.WHITE);
        openMainBtn.setPadding(40, 20, 40, 20);
        layout.addView(openMainBtn);

        openMainBtn.setOnClickListener(v -> {
            // Cancel the auto-close
            handler.removeCallbacks(autoCloseRunnable);

            // Open MainActivity
            Intent intent = new Intent(StealthActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Finish this activity
            finish();
        });

        setContentView(layout);

        // Auto-close after 30 seconds
        autoCloseRunnable = this::finish;
        handler.postDelayed(autoCloseRunnable, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Move to background when paused
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup the handler
        handler.removeCallbacks(autoCloseRunnable);
    }
}
