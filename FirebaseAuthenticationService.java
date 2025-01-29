package com.example.flight.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseAuthenticationService {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;
    private final Map<String, String> authenticatedDoctors;

    public FirebaseAuthenticationService() {
        try {
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-config.json");
            if (serviceAccount == null) {
                throw new RuntimeException("Firebase configuration file not found!");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options, "doctorapp");
            }

            this.firebaseAuth = FirebaseAuth.getInstance(FirebaseApp.getInstance("doctorapp"));


            serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-config.json");
            this.firestore = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()
                    .getService();

            this.authenticatedDoctors = new HashMap<>();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    public FirebaseToken verifyToken(String token) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(token);
    }

    public boolean isAuthenticated(String token) {
        try {
            FirebaseToken firebaseToken = verifyToken(token);
            return firebaseToken != null;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }

    public String getUserEmailFromToken(String token) {
        try {
            FirebaseToken firebaseToken = verifyToken(token);
            return firebaseToken.getEmail();
        } catch (FirebaseAuthException e) {
            return null;
        }
    }

    public void storeAuthenticatedDoctor(String token) {
        try {
            String email = getUserEmailFromToken(token);
            if (email != null) {
                authenticatedDoctors.put(email, token);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to store authenticated doctor: " + e.getMessage());
        }
    }

    public boolean isDoctorAuthenticated(String email) {
        return authenticatedDoctors.containsKey(email);
    }

    public DoctorDetails createDoctorDetails(String email, String doctorName, String areaOfInterest, String availableDays, String availableHours, String address, String city) {
        if (!isDoctorAuthenticated(email)) {
            throw new IllegalStateException("Doctor must authenticate with Google first.");
        }

        try {
            DoctorDetails doctorDetails = new DoctorDetails(email, doctorName, areaOfInterest, availableDays, availableHours, address, city, false);
            firestore.collection("doctors").document(email).set(doctorDetails).get();
            return doctorDetails;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save doctor details: " + e.getMessage());
        }
    }

    public void registerPatient(String token) {
        try {
            FirebaseToken firebaseToken = verifyToken(token);
            String email = firebaseToken.getEmail();

            if (email == null) {
                throw new IllegalArgumentException("Token does not contain a valid email.");
            }

            Map<String, Object> patientData = new HashMap<>();
            patientData.put("email", email);
            patientData.put("registrationTimestamp", System.currentTimeMillis());

            firestore.collection("patients").document(email).set(patientData).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register patient: " + e.getMessage(), e);
        }
    }
    public Firestore getFirestore() {
        return this.firestore;
    }


    public static class DoctorDetails implements Serializable {
        private String email;
        private String doctorName;
        private String areaOfInterest;
        private String availableDays;
        private String availableHours;
        private String city;
        private String address;
        private boolean approved;

        public DoctorDetails() {
        }

        public DoctorDetails(String email, String doctorName, String areaOfInterest, String availableDays, String availableHours, String address, String city, boolean approved) {
            this.email = email;
            this.doctorName = doctorName;
            this.areaOfInterest = areaOfInterest;
            this.availableDays = availableDays;
            this.availableHours = availableHours;
            this.city = city;
            this.address = address;
            this.approved = approved;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDoctorName() {
            return doctorName;
        }

        public void setDoctorName(String doctorName) {
            this.doctorName = doctorName;
        }

        public String getAreaOfInterest() {
            return areaOfInterest;
        }

        public void setAreaOfInterest(String areaOfInterest) {
            this.areaOfInterest = areaOfInterest;
        }

        public String getAvailableDays() {
            return availableDays;
        }

        public void setAvailableDays(String availableDays) {
            this.availableDays = availableDays;
        }

        public String getAvailableHours() {
            return availableHours;
        }

        public void setAvailableHours(String availableHours) {
            this.availableHours = availableHours;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {this.city = city;
        }
        public boolean isApproved() { return approved; }

        public void setApproved(boolean approved) { this.approved = approved; }
    }
    }

