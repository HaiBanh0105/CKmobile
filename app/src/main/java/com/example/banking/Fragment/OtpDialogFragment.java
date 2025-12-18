package com.example.banking.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.banking.EmailService;
import com.example.banking.R;
import com.example.banking.SessionManager;

import java.util.Random;

public class OtpDialogFragment extends DialogFragment {

    public interface OtpCallback {
        void onOtpSuccess();
        void onOtpFailed();
    }

    private OtpCallback callback;
    private String currentOtp;
    private EditText[] otpFields = new EditText[6];
    private TextView btnResend, tvOtpTitle, tvMessage;

    private boolean isPinVerified = false;
    private boolean canResend = false;
    private boolean isInternalStop = false; // chặn loop khi paste

    private final String savedPin = SessionManager.getInstance().getPinNumber();
    private final String userEmail = SessionManager.getInstance().getEmail();

    public OtpDialogFragment(OtpCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_otp, null);

        tvOtpTitle = view.findViewById(R.id.tvOtpTitle);
        tvMessage = view.findViewById(R.id.tvMessage);
        btnResend = view.findViewById(R.id.btnResend);

        otpFields[0] = view.findViewById(R.id.otp1);
        otpFields[1] = view.findViewById(R.id.otp2);
        otpFields[2] = view.findViewById(R.id.otp3);
        otpFields[3] = view.findViewById(R.id.otp4);
        otpFields[4] = view.findViewById(R.id.otp5);
        otpFields[5] = view.findViewById(R.id.otp6);

        setupInitialState();
        setupOtpLogic();

        btnResend.setOnClickListener(v -> {
            if (canResend && isPinVerified) {
                sendOtpEmail();
                startResendTimer();
            }
        });

        builder.setView(view)
                .setPositiveButton("Xác nhận", null)
                .setNegativeButton("Hủy", (d, w) -> callback.onOtpFailed());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> handleVerification())
        );

        return dialog;
    }

    private void setupInitialState() {
        isPinVerified = false;
        tvOtpTitle.setText("Xác thực mã PIN");
        tvMessage.setText("Vui lòng nhập mã PIN để tiếp tục");
        btnResend.setVisibility(View.GONE);
        clearFields();
    }

    private void setupOtpLogic() {
        for (int i = 0; i < 6; i++) {
            final int index = i;

            otpFields[i].setInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
            );

            // 🔒 CHỈ CHO PHÉP SỐ (0–9), CHẶN CHỮ / DẤU . / KÝ TỰ ĐẶC BIỆT
            otpFields[i].setFilters(new InputFilter[]{
                    (source, start, end, dest, dstart, dend) -> {
                        for (int j = start; j < end; j++) {
                            if (!Character.isDigit(source.charAt(j))) {
                                return "";
                            }
                        }
                        return null;
                    }
            });

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isInternalStop) return;

                    // XỬ LÝ PASTE 6 SỐ
                    if (s.length() > 1) {
                        String pasted = s.toString();
                        if (pasted.length() >= 6) {
                            fillOtpFields(pasted.substring(0, 6));
                        }
                    }
                    // CHUYỂN FOCUS
                    else if (s.length() == 1 && index < 5) {
                        otpFields[index + 1].requestFocus();
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });

            // BACKSPACE → LÙI FOCUS
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {

                    if (otpFields[index].getText().length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void fillOtpFields(String text) {
        isInternalStop = true;
        char[] chars = text.toCharArray();
        for (int i = 0; i < 6; i++) {
            otpFields[i].setText(String.valueOf(chars[i]));
        }
        otpFields[5].requestFocus();
        isInternalStop = false;
    }

    private void handleVerification() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpFields) sb.append(et.getText().toString());
        String input = sb.toString();

        if (input.length() < 6) {
            Toast.makeText(getContext(), "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPinVerified) {
            if (input.equals(savedPin)) {
                isPinVerified = true;
                tvOtpTitle.setText("Xác thực OTP");
                tvMessage.setText("Mã đã gửi đến: " + userEmail);
                btnResend.setVisibility(View.VISIBLE);
                clearFields();
                sendOtpEmail();
                startResendTimer();
            } else {
                Toast.makeText(getContext(), "PIN không đúng!", Toast.LENGTH_SHORT).show();
                clearFields();
            }
        } else {
            if (input.equals(currentOtp)) {
                callback.onOtpSuccess();
                dismiss();
            } else {
                Toast.makeText(getContext(), "OTP sai!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendOtpEmail() {
        currentOtp = String.valueOf(100000 + new Random().nextInt(900000));
        EmailService.sendEmail(
                getContext(),
                userEmail,
                "Mã OTP",
                "Mã của bạn: " + currentOtp,
                null
        );
    }

    private void startResendTimer() {
        canResend = false;
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                btnResend.setText("Gửi lại mã (" + millisUntilFinished / 1000 + "s)");
                btnResend.setEnabled(false);
            }

            public void onFinish() {
                btnResend.setText("Gửi lại mã");
                btnResend.setEnabled(true);
                canResend = true;
            }
        }.start();
    }

    private void clearFields() {
        isInternalStop = true;
        for (EditText et : otpFields) et.setText("");
        otpFields[0].requestFocus();
        isInternalStop = false;
    }
}
