package com.example.banking.util;

import android.view.MotionEvent;
import android.view.View;

public class ClickEffectUtil {
    public static void apply(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(100)
                            .start();
                    return true;

                case MotionEvent.ACTION_UP:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();

                    v.performClick(); // ⭐ BẮT BUỘC
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                    return true;
            }
            return false;
        });
    }

}
