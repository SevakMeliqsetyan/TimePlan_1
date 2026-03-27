package sevak.meliqsetyan.samsung_project_2.util;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailHelper {

    // ВНИМАНИЕ: Здесь нужно указать вашу почту и ПАРОЛЬ ПРИЛОЖЕНИЯ (не обычный пароль!)
    private static final String SENDER_EMAIL = "your_email@gmail.com"; 
    private static final String APP_PASSWORD = "your_app_password"; 

    public static void sendEmail(String recipientEmail, String taskTitle) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Напоминание о задаче: " + taskTitle);
            message.setText("Здравствуйте!\n\nНапоминаем, что через 30 минут у вас запланирована задача: " + taskTitle);

            new Thread(() -> {
                try {
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}