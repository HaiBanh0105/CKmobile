package com.example.banking.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.banking.R;

public abstract class BaseSecureActivity extends AppCompatActivity {

    private FrameLayout loadingOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đăng ký callback cho back button + back gesture
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Nếu đang loading thì chặn
                        if (isLoading()) return;

                        // Không loading → cho back bình thường
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
        );
    }

    protected void initLoading(View rootView) {
        loadingOverlay = rootView.findViewById(R.id.loadingOverlay);
    }

    protected void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    protected boolean isLoading() {
        return loadingOverlay != null
                && loadingOverlay.getVisibility() == View.VISIBLE;
    }
}

