package com.example.flight.services;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final Firestore firestore;

    public NotificationService() {
        FirebaseApp doctorApp = FirebaseApp.getInstance("doctorapp");
        this.firestore = FirestoreClient.getFirestore(doctorApp);
    }

    public void publishNotificationToQueue(String email, String day, String hour, String doctorName) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                String queueName = "appointment_notifications";
                channel.queueDeclare(queueName, true, false, false, null);

                String message = String.format(
                        "{\"email\":\"%s\",\"message\":\"Please confirm your appointment with Dr. %s on %s at %s.\"}",
                        email, doctorName, day, hour
                );

                channel.basicPublish("", queueName, null, message.getBytes());
                System.out.println("Notification sent to queue: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to publish notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishReviewNotificationToQueue(String email, String doctorName) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                String queueName = "review_notifications";
                channel.queueDeclare(queueName, true, false, false, null);


                String message = String.format(
                        "{\"email\":\"%s\",\"message\":\"Thank you for visiting Dr. %s. Please take a moment to rate your experience.\"}",
                        email, doctorName
                );


                channel.basicPublish("", queueName, null, message.getBytes());
                System.out.println("Review notification sent to queue: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to publish review notification: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void sendDailyReminders() {
        try {
            List<QueryDocumentSnapshot> unconfirmedAppointments = firestore.collection("tempAppointments")
                    .whereEqualTo("status", "Not Confirmed")
                    .get()
                    .get()
                    .getDocuments();

            for (QueryDocumentSnapshot doc : unconfirmedAppointments) {
                String email = (String) doc.get("patientEmail");
                String day = (String) doc.get("day");
                String hour = (String) doc.get("hour");
                String doctorName = (String) doc.get("doctorName");

                if (email != null && day != null && hour != null && doctorName != null) {
                    publishNotificationToQueue(email, day, hour, doctorName);
                } else {
                    System.err.println("Missing required fields in unconfirmed appointment: " + doc.getId());
                }
            }

            System.out.println("Daily reminders sent successfully!");
        } catch (Exception e) {
            System.err.println("Error sending daily reminders: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
