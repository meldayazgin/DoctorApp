package com.example.flight.controllers;

import com.example.flight.dto.request.AppointmentRequest;
import com.example.flight.dto.request.DoctorSearchRequest;
import com.example.flight.services.DoctorService;
import com.example.flight.services.FirebaseAuthenticationService;
import com.example.flight.services.FirebaseAuthenticationService.DoctorDetails;
import com.example.flight.services.NotificationService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final Firestore firestore;
    private final NotificationService notificationService;

    @Autowired
    public DoctorController(DoctorService doctorService, NotificationService notificationService) {
        this.doctorService = doctorService;
        this.notificationService = notificationService;
        FirebaseApp doctorApp = FirebaseApp.getInstance("doctorapp");
        this.firestore = FirestoreClient.getFirestore(doctorApp);
    }

    @GetMapping("/search")
    public PaginatedDoctors searchDoctors(@RequestParam String searchTerm,
                                          @RequestParam int page,
                                          @RequestParam(required = false) String city,
                                          @RequestParam int size) {

        DoctorSearchRequest doctorSearchRequest = new DoctorSearchRequest();
        doctorSearchRequest.setAreaOfInterest(searchTerm);
        doctorSearchRequest.setCity(city);
        doctorSearchRequest.setPage(page);
        doctorSearchRequest.setSize(size);

        List<DoctorDetails> doctors = doctorService.queryDoctors(doctorSearchRequest);
        System.out.println("City in search query: " + city);
        return new PaginatedDoctors(doctors, doctorSearchRequest.getPage(), doctorSearchRequest.getSize());
    }

    @PostMapping("/tempAppointments")
    public ResponseEntity<String> saveTempAppointment(@RequestBody AppointmentRequest request) {
        try {
            if (request.getDoctorEmail() == null || request.getPatientEmail() == null ||
                    request.getHour() == null || request.getDay() == null ||
                    request.getAreaOfInterest() == null) {
                System.err.println("Missing required fields in request: " + request);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required fields.");
            }

            System.out.println("Incoming Appointment Request: " + request);
            System.out.println("Incoming Appointment Request: " + request);


            System.out.println("Doctor Name: " + request.getDoctorName());
            System.out.println("Patient Email: " + request.getPatientEmail());
            System.out.println("Doctor Email: " + request.getDoctorEmail());
            System.out.println("City: " + request.getCity());
            firestore.collection("tempAppointments").add(Map.of(
                    "areaOfInterest", request.getAreaOfInterest(),
                    "day", request.getDay(),
                    "doctorEmail", request.getDoctorEmail(),
                    "doctorName", request.getDoctorName(),
                    "hour", request.getHour(),
                    "patientEmail", request.getPatientEmail(),
                    "patientName", request.getPatientName(),
                    "status", "Not Confirmed",
                    "visitStatus", "Not Completed",
                    "createdAt", FieldValue.serverTimestamp()
            ));

            notificationService.publishNotificationToQueue(request.getPatientEmail(), request.getDay(), request.getHour(), request.getDoctorName());

            return ResponseEntity.ok("Temporary appointment stored successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store temporary appointment.");
        }
    }

    @GetMapping("/tempAppointments")
    public ResponseEntity<List<Map<String, Object>>> getTempAppointments(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp")).verifyIdToken(token);
            String userEmail = decodedToken.getEmail();

            System.out.println("Decoded Token Email: " + userEmail);
            System.out.println("Querying tempAppointments collection...");

            List<Map<String, Object>> tempAppointments = firestore.collection("tempAppointments")
                    .whereEqualTo("patientEmail", userEmail)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        return data;
                    })
                    .collect(Collectors.toList());

            System.out.println("Filtered tempAppointments: " + tempAppointments.size());
            return ResponseEntity.ok(tempAppointments);
        } catch (FirebaseAuthException e) {
            System.err.println("FirebaseAuthException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @DeleteMapping("/tempAppointments/{id}")
    public ResponseEntity<String> deleteAppointment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id) {
        try {
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp")).verifyIdToken(token);
            String userEmail = decodedToken.getEmail();

            Map<String, Object> appointment = firestore.collection("tempAppointments")
                    .document(id)
                    .get()
                    .get()
                    .getData();

            if (appointment == null || !userEmail.equals(appointment.get("patientEmail"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to delete this appointment.");
            }

            firestore.collection("tempAppointments").document(id).delete();
            System.out.println("Appointment with ID " + id + " deleted.");
            return ResponseEntity.ok("Appointment deleted successfully.");
        } catch (FirebaseAuthException e) {
            System.err.println("FirebaseAuthException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete appointment.");
        }
    }
    @PostMapping("/approve/{doctorId}")
    public ResponseEntity<String> approveDoctor(@PathVariable String doctorId) {
        try {

            var doctorDoc = firestore.collection("doctors").document(doctorId).get().get();

            if (!doctorDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found.");
            }

            System.out.println("Doctor document data: " + doctorDoc.getData());

            firestore.collection("doctors")
                    .document(doctorId)
                    .update("approved", true)
                    .get();

            return ResponseEntity.ok("Doctor approved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to approve doctor.");
        }
    }


    @PostMapping("/tempAppointments/{id}/complete")
    public ResponseEntity<String> completeVisit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id) {
        try {
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp")).verifyIdToken(token);
            String userEmail = decodedToken.getEmail();

            Map<String, Object> appointment = firestore.collection("tempAppointments")
                    .document(id)
                    .get()
                    .get()
                    .getData();

            if (appointment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found.");
            }

            String patientEmail = (String) appointment.get("patientEmail");
            String status = (String) appointment.get("status");

            if (!userEmail.equals(patientEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to complete this appointment.");
            }

            if (!"Confirmed".equals(status)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Appointment must be confirmed before marking it as completed.");
            }

            firestore.collection("tempAppointments").document(id).update("visitStatus", "Completed").get();
            String doctorName = (String) appointment.get("doctorName");
            notificationService.publishReviewNotificationToQueue(patientEmail, doctorName);

            System.out.println("Visit for appointment ID " + id + " marked as completed and notification sent.");
            return ResponseEntity.ok("Visit marked as completed.");
        } catch (FirebaseAuthException e) {
            System.err.println("FirebaseAuthException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark visit as completed.");
        }
    }




    @GetMapping("/confirmedAppointments")
    public ResponseEntity<List<Map<String, Object>>> getConfirmedAppointments(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp"))
                    .verifyIdToken(token);

            String userEmail = decodedToken.getEmail();
            System.out.println("Decoded Token Email: " + userEmail);

            List<Map<String, Object>> confirmedAppointments = firestore.collection("tempAppointments")
                    .whereEqualTo("status", "Confirmed")
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        return data;
                    })
                    .collect(Collectors.toList());

            System.out.println("Confirmed Appointments: " + confirmedAppointments.size());
            return ResponseEntity.ok(confirmedAppointments);
        } catch (FirebaseAuthException e) {
            System.err.println("FirebaseAuthException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/appointments")
    public ResponseEntity<String> confirmAppointment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> tempAppointment) {
        try {
            String doctorEmail = (String) tempAppointment.get("doctorEmail");
            String confirmedHour = (String) tempAppointment.get("hour");
            String patientEmail = (String) tempAppointment.get("patientEmail");

            if (doctorEmail == null || doctorEmail.isEmpty() || confirmedHour == null || confirmedHour.isEmpty() || patientEmail == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing required fields in appointment.");
            }

            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp")).verifyIdToken(token);
            String userEmail = decodedToken.getEmail();

            if (!patientEmail.equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User email does not match token.");
            }

            String docId = (String) tempAppointment.get("id");
            firestore.collection("tempAppointments")
                    .document(docId)
                    .update("status", "Confirmed")
                    .get();

            return ResponseEntity.ok("Appointment confirmed and doctor's available hours updated.");
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to confirm appointment.");
        }
    }

    @Scheduled(fixedRate = 86400000)
    public void sendDailyReminders() {
        notificationService.sendDailyReminders();
    }

    public static class PaginatedDoctors {
        private List<DoctorDetails> doctors;
        private int page;
        private int size;

        public PaginatedDoctors(List<DoctorDetails> doctors, int page, int size) {
            this.doctors = doctors;
            this.page = page;
            this.size = size;
        }

        public List<DoctorDetails> getDoctors() {
            return doctors;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }
    }
}
