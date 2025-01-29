package com.example.flight.services;

import com.example.flight.dto.request.AppointmentRequest;
import com.example.flight.dto.request.DoctorSearchRequest;
import com.example.flight.services.FirebaseAuthenticationService.DoctorDetails;
import com.example.flight.repositories.DoctorRepository;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final Firestore firestore;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;

        FirebaseApp doctorApp = FirebaseApp.getInstance("doctorapp");
        this.firestore = FirestoreClient.getFirestore(doctorApp);
    }

    public List<DoctorDetails> queryDoctors(DoctorSearchRequest doctorSearchRequest) {
        PageRequest pageRequest = PageRequest.of(doctorSearchRequest.getPage() - 1, doctorSearchRequest.getSize());

        String areaOfInterest = doctorSearchRequest.getAreaOfInterest();
        String city = doctorSearchRequest.getCity();
        String doctorName = doctorSearchRequest.getDoctorName();

        if (areaOfInterest != null && !areaOfInterest.isEmpty() && city != null && !city.isEmpty()) {
            return doctorRepository.findDoctorsByAreaOfInterestAndCityAndDoctorName(areaOfInterest, city, doctorName, pageRequest);
        }

        if (areaOfInterest != null && !areaOfInterest.isEmpty()) {
            return doctorRepository.findDoctorsByAreaOfInterest(areaOfInterest, pageRequest);
        }

        if (city != null && !city.isEmpty()) {
            return doctorRepository.findDoctorsByCity(city, pageRequest);
        }
        if (doctorName != null && !doctorName.isEmpty()) {
            return doctorRepository.findDoctorsByDoctorName(doctorName, pageRequest);
        }

        return doctorRepository.findAll(pageRequest);
    }

    public boolean createAppointment(AppointmentRequest appointmentRequest) {
        try {
            Map<String, Object> doctor = firestore.collection("doctors")
                    .document(appointmentRequest.getDoctorEmail())
                    .get()
                    .get()
                    .getData();

            if (doctor == null) {
                return false;
            }

            List<String> availableHours = new ArrayList<>(List.of(doctor.get("availableHours").toString().split(",")));
            if (!availableHours.contains(appointmentRequest.getHour())) {
                return false;
            }
            availableHours.remove(appointmentRequest.getHour());
            firestore.collection("doctors")
                    .document(appointmentRequest.getDoctorEmail())
                    .update("availableHours", String.join(",", availableHours));


            firestore.collection("appointments").add(Map.of(
                    "doctorEmail", appointmentRequest.getDoctorEmail(),
                    "doctorName", appointmentRequest.getDoctorName(),
                    "patientEmail", appointmentRequest.getPatientEmail(),
                    "patientName", appointmentRequest.getPatientName(),
                    "hour", appointmentRequest.getHour(),
                    "day", appointmentRequest.getDay(),
                    "specialty", doctor.get("areaOfInterest").toString(),
                    "createdAt", FieldValue.serverTimestamp()
            ));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean removeAvailableHourByEmail(String doctorEmail, String hourToRemove) {
        try {
            List<Map<String, Object>> doctorDocs = firestore.collection("doctors")
                    .whereEqualTo("email", doctorEmail)
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

            if (doctorDocs.isEmpty()) {
                System.err.println("Doctor not found for email: " + doctorEmail);
                return false;
            }

            Map<String, Object> doctor = doctorDocs.get(0);
            String documentId = (String) doctor.get("id");

            String availableHoursStr = (String) doctor.get("availableHours");
            if (availableHoursStr == null || availableHoursStr.isEmpty()) {
                System.err.println("Available hours not found or empty for doctor: " + doctorEmail);
                return false;
            }

            List<String> availableHours = Arrays.stream(availableHoursStr.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            System.out.println("Available Hours Before Update: " + availableHours);

            if (!availableHours.contains(hourToRemove)) {
                System.err.println("Hour not found in available hours: " + hourToRemove);
                return false;
            }

            availableHours.remove(hourToRemove);
            firestore.collection("doctors")
                    .document(documentId)
                    .update("availableHours", String.join(",", availableHours))
                    .get();

            System.out.println("Successfully removed hour " + hourToRemove + " for doctor " + doctorEmail);
            System.out.println("Available Hours After Update: " + availableHours);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
