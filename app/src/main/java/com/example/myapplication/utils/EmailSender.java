package com.example.myapplication.utils;

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

import android.os.Handler;
import android.os.Looper;

import com.example.myapplication.BuildConfig;

public class EmailSender {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587"; // TLS Port

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface EmailCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void sendOtpEmail(String recipientEmail, String otpCode, EmailCallback callback) {
        executorService.execute(() -> {
            try {
                // Configure SMTP properties
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);

                // Create Session
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(BuildConfig.SENDER_EMAIL, BuildConfig.SENDER_PASSWORD);
                    }
                });

                // Create Message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(BuildConfig.SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("AutoQuiz - Mã xác thực OTP của bạn");
                
                String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;\">" +
                        "<h2 style=\"color: #E84315;\">Xác thực tài khoản AutoQuiz</h2>" +
                        "<p>Chào bạn,</p>" +
                        "<p>Mã OTP để hoàn tất quá trình của bạn là:</p>" +
                        "<div style=\"font-size: 32px; font-weight: bold; background: #f4f4f4; padding: 10px; text-align: center; color: #6200EE; letter-spacing: 5px;\">" +
                        otpCode + "</div>" +
                        "<p>Mã này có hiệu lực trong <b>60 giây</b>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
                        "<br><p>Trân trọng,<br>Đội ngũ AutoQuiz</p></div>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                // Send Email
                Transport.send(message);

                // Notify success on main thread
                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                // Notify failure on main thread
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }
}
