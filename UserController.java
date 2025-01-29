package com.example.flight.controllers;

import com.example.flight.dto.request.DoctorRegistrationRequest;
import com.example.flight.dto.request.LoginRequest;
import com.example.flight.services.FirebaseAuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final FirebaseAuthenticationService authService;

    public UserController(FirebaseAuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (authService.isAuthenticated(request.getToken())) {
                String email = authService.getUserEmailFromToken(request.getToken());
                Map<String, String> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("email", email);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/register/doctor/start")
    public ResponseEntity<?> startDoctorRegistration(@RequestBody LoginRequest request) {
        try {
            if (authService.isAuthenticated(request.getToken())) {
                String email = authService.getUserEmailFromToken(request.getToken());
                authService.storeAuthenticatedDoctor(request.getToken());
                Map<String, String> response = new HashMap<>();
                response.put("message", "Google authentication successful");
                response.put("email", email);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/register/doctor/complete")
    public ResponseEntity<?> completeDoctorRegistration(@RequestBody DoctorRegistrationRequest request) {
        try {
            System.out.println("Doctor Registration Request Payload: " + request);
            String email = authService.getUserEmailFromToken(request.getToken());

            FirebaseAuthenticationService.DoctorDetails doctorDetails = authService.createDoctorDetails(
                    email,
                    request.getDoctorName(),
                    request.getAreaOfInterest(),
                    request.getAvailableDays(),
                    request.getAvailableHours(),
                    request.getAddress(),
                    request.getCity()
            );

            return ResponseEntity.ok("Doctor registration completed successfully: " + doctorDetails.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }



    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@RequestBody LoginRequest request) {
        try {
            authService.registerPatient(request.getToken());
            return ResponseEntity.ok("Patient registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }
}
