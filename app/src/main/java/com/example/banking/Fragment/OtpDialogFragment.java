package com.example.banking.Fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
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
import com.example.banking.model.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private final String userId = SessionManager.getInstance().getUserId();

    public OtpDialogFragment(OtpCallback callback) {
        this.callback = callback;
    }

    private FirebaseFirestore db;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_otp, null);

        db = FirebaseFirestore.getInstance();

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


    private boolean isRegisterMode = false;

    public void setRegisterMode(boolean isRegister) {
        this.isRegisterMode = isRegister;
    }

    private void setupInitialState() {
        if (isRegisterMode) {
            // NẾU LÀ ĐĂNG KÝ: Bỏ qua bước nhập PIN
            isPinVerified = true; // Coi như đã qua bước PIN
            tvOtpTitle.setText("Xác thực Email");
            tvMessage.setText("Đang gửi mã OTP...");
            btnResend.setVisibility(View.VISIBLE);

            // Tự động gửi OTP luôn
            sendOtpEmail();
            startResendTimer();

            // Focus vào ô nhập OTP
            clearFields();
        }
        else {
            isPinVerified = false;
            tvOtpTitle.setText("Xác thực mã PIN");
            tvMessage.setText("Vui lòng nhập mã PIN để tiếp tục");
            btnResend.setVisibility(View.GONE);
            clearFields();
        }
    }

    private void setupOtpLogic() {
        for (int i = 0; i < 6; i++) {
            final int index = i;

            otpFields[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            otpFields[i].setTransformationMethod(PasswordTransformationMethod.getInstance());

            otpFields[i].setFilters(new InputFilter[]{
                    (source, start, end, dest, dstart, dend) -> {
                        if (isInternalStop) return null;

                        // Chỉ cho phép số
                        for (int j = start; j < end; j++) {
                            if (!Character.isDigit(source.charAt(j))) return "";
                        }

                        // Chặn nhập thêm nếu tổng 6 ký tự
                        String totalInput = getCurrentOtpInput();
                        if (totalInput.length() >= 6) return "";

                        return null;
                    }
            });

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isInternalStop) return;

                    String totalInput = getCurrentOtpInput();

                    // Xử lý paste >=6 số
                    if (s.length() > 1) {
                        String pasted = s.toString().replaceAll("\\D", "");
                        if (pasted.length() >= 6) {
                            fillOtpFields(pasted.substring(0, 6));
                            return;
                        }
                    }

                    // Nếu tổng >=6 thì chặn các ô còn lại
                    if (totalInput.length() > 6) {
                        fillOtpFields(totalInput.substring(0, 6));
                        return;
                    }

                    // Chuyển focus nếu nhập 1 ký tự
                    if (s.length() == 1 && index < 5) {
                        otpFields[index + 1].requestFocus();
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            });

            // BACKSPACE → lùi focus
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
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

    // Lấy chuỗi OTP tổng thể
    private String getCurrentOtpInput() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpFields) sb.append(et.getText().toString());
        return sb.toString();
    }

    private void fillOtpFields(String text) {
        isInternalStop = true;
        char[] chars = text.toCharArray();
        for (int i = 0; i < 6; i++) {
            otpFields[i].setText(String.valueOf(chars[i]));
        }
        otpFields[5].requestFocus(); // focus cuối cùng
        isInternalStop = false;
    }

    private void handleVerification() {
        String input = getCurrentOtpInput();

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
                db.collection("Users")
                        .document(userId)
                        .update(
                                "pin_fail_count", 0
                        );
            } else {
                increaseFailCount("pin");
                Toast.makeText(getContext(), "PIN không đúng!", Toast.LENGTH_SHORT).show();
                clearFields();
            }
        } else {
            if (input.equals(currentOtp)) {
                db.collection("Users")
                        .document(userId)
                        .update(
                                "otp_fail_count", 0
                        );
                callback.onOtpSuccess();
                dismiss();
            } else {
                increaseFailCount("otp");
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

    private void increaseFailCount(String type) {
        db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(
                    db.collection("Users").document(userId)
            );

            long otpFail = doc.getLong("otp_fail_count") == null ? 0 : doc.getLong("otp_fail_count");
            long pinFail = doc.getLong("pin_fail_count") == null ? 0 : doc.getLong("pin_fail_count");

            if ("otp".equals(type)) otpFail++;
            if ("pin".equals(type)) pinFail++;

            boolean ekycRequired = otpFail >= 3 || pinFail >= 3;

            transaction.update(doc.getReference(),
                    "otp_fail_count", otpFail,
                    "pin_fail_count", pinFail,
                    "ekyc_required", ekycRequired,
                    "last_security_violation", FieldValue.serverTimestamp()
            );

            return ekycRequired;
        }).addOnSuccessListener(ekycRequired -> {
            if (ekycRequired) forceEkyc();
        });
    }

    private void forceEkyc() {
        Toast.makeText(getContext(),
                "Bạn đã nhập sai quá nhiều lần. Vui lòng xác minh eKYC.",
                Toast.LENGTH_LONG).show();

        callback.onOtpFailed();
        dismiss();
    }


}
