package com.example.pms_nhom9.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pms_nhom9.R;
import java.util.Locale;

public class FocusModeActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    private TextView tvFocusTitle, tvFocusTimer;
    private Button btnStopFocus;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 1500000; // 25 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        tvFocusTitle = findViewById(R.id.tvFocusTitle);
        tvFocusTimer = findViewById(R.id.tvFocusTimer);
        btnStopFocus = findViewById(R.id.btnStopFocus);

        String eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        if (eventTitle != null) {
            tvFocusTitle.setText("Đang tập trung: " + eventTitle);
        }

        startTimer();

        btnStopFocus.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            finish();
        });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                tvFocusTimer.setText("00:00");
                // Optional: add notification or sound
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvFocusTimer.setText(timeLeftFormatted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
