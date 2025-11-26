
package com.example.banking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    // CẤU HÌNH EMAIL CỦA NGÂN HÀNG (NGƯỜI GỬI)
    // Lưu ý: Đây phải là "Mật khẩu ứng dụng" (App Password), KHÔNG phải mật khẩu đăng nhập Gmail thường.
    private static final String SENDER_EMAIL = "vodathai91thcsduclap@gmail.com";
    private static final String SENDER_PASSWORD = "evxb qnnf tist kvzi";

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }


    public static void sendEmail(Context context,
                                 String recipientEmail,
                                 String subject,
                                 String messageBody,
                                 EmailCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 1. Cấu hình Server Gmail (SMTP)
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                // 2. Tạo phiên làm việc (Session)
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                // 3. Tạo nội dung Email
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(messageBody);

                // 4. Gửi Email
                Transport.send(message);

                // 5. Quay về luồng chính báo thành công
                handler.post(() -> {
                    Toast.makeText(context, "Đã gửi Email mật khẩu!", Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onSuccess();
                });

            } catch (MessagingException e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(context, "Gửi Email thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onFailure(e.getMessage());
                });
            }
        });
    }

}