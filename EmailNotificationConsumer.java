package com.example.flight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class EmailNotificationConsumer {

    private static final String QUEUE_NAME = "appointment_notifications";
    private static final String REVIEW_QUEUE_NAME = "review_notifications";
    private static final String MAIL_SERVER = "smtp.gmail.com";
    private static final int MAIL_PORT = 587;
    private static final boolean MAIL_USE_TLS = true;
    private static final String MAIL_USERNAME = "yazginmelda@gmail.com";
    private static final String MAIL_PASSWORD = "whxl pygf ifbz levo";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);

        System.out.println("Connecting to RabbitMQ...");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {


            channel.basicQos(1);


            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueDeclare(REVIEW_QUEUE_NAME, true, false, false, null);

            System.out.println("Connected to RabbitMQ. Waiting for messages on queues: "
                    + QUEUE_NAME + " and " + REVIEW_QUEUE_NAME);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String queueName = delivery.getEnvelope().getRoutingKey();

                System.out.println("Received message from queue [" + queueName + "]: " + messageBody);

                try {
                    System.out.println("Simulating processing delay...");
                    Thread.sleep(5000);

                    Map<String, String> messageData = objectMapper.readValue(messageBody, Map.class);


                    if (QUEUE_NAME.equals(queueName)) {
                        handleAppointmentNotification(messageData);
                    } else if (REVIEW_QUEUE_NAME.equals(queueName)) {
                        handleReviewNotification(messageData);
                    }


                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    System.out.println("Message processed successfully and acknowledged.");
                } catch (Exception e) {
                    System.err.println("Failed to process message: " + e.getMessage());
                    e.printStackTrace();


                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };


            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
                System.out.println("Consumer for " + QUEUE_NAME + " canceled.");
            });

            channel.basicConsume(REVIEW_QUEUE_NAME, false, deliverCallback, consumerTag -> {
                System.out.println("Consumer for " + REVIEW_QUEUE_NAME + " canceled.");
            });


            System.out.println("Consumer is running. Press Ctrl+C to exit.");
            synchronized (EmailNotificationConsumer.class) {
                EmailNotificationConsumer.class.wait();
            }

        } catch (Exception e) {
            System.err.println("Failed to connect to RabbitMQ or initialize consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void handleAppointmentNotification(Map<String, String> messageData) throws Exception {

        String recipientEmail = messageData.get("email");
        String notificationContent = messageData.get("message");

        if (recipientEmail == null || notificationContent == null) {
            throw new IllegalArgumentException("Invalid appointment notification structure: Missing 'email' or 'message'.");
        }

        sendEmail(recipientEmail, "Appointment Confirmation Reminder", notificationContent);
        System.out.println("Appointment notification email sent to: " + recipientEmail);
    }

    private static void handleReviewNotification(Map<String, String> messageData) throws Exception {
        String recipientEmail = messageData.get("email");
        String notificationContent = messageData.get("message");

        if (recipientEmail == null || notificationContent == null) {
            throw new IllegalArgumentException("Invalid review notification structure: Missing 'email' or 'message'.");
        }

        sendEmail(recipientEmail, "We Value Your Feedback", notificationContent);
        System.out.println("Review notification email sent to: " + recipientEmail);
    }

    private static void sendEmail(String recipientEmail, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.host", MAIL_SERVER);
        props.put("mail.smtp.port", MAIL_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(MAIL_USE_TLS));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MAIL_USERNAME, MAIL_PASSWORD);
            }
        });

        try {
            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(MAIL_USERNAME));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            email.setSubject(subject);
            email.setText(content);

            Transport.send(email);
            System.out.println("Email sent successfully to: " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
