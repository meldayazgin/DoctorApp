package com.example.flight.repositories;

import com.example.flight.services.FirebaseAuthenticationService.DoctorDetails;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class DoctorRepository {

    private final Firestore firestore;

    public DoctorRepository() {
        FirebaseApp doctorApp = FirebaseApp.getInstance("doctorapp");
        this.firestore = FirestoreClient.getFirestore(doctorApp);
    }

    public List<DoctorDetails> findDoctorsByAreaOfInterestAndCityAndDoctorName(String areaOfInterest, String city, String doctorName, PageRequest pageRequest) {
        return executeQuery(
                firestore.collection("doctors")
                        .whereEqualTo("areaOfInterest", areaOfInterest)
                        .whereEqualTo("city", city)
                        .whereEqualTo("doctorName", doctorName),
                pageRequest
        );
    }

    public List<DoctorDetails> findDoctorsByAreaOfInterest(String areaOfInterest, PageRequest pageRequest) {
        return executeQuery(
                firestore.collection("doctors")
                        .whereEqualTo("areaOfInterest", areaOfInterest),
                pageRequest
        );
    }

    public List<DoctorDetails> findDoctorsByCity(String city, PageRequest pageRequest) {
        return executeQuery(
                firestore.collection("doctors")
                        .whereEqualTo("city", city),
                pageRequest
        );
    }

    public List<DoctorDetails> findDoctorsByDoctorName(String doctorName, PageRequest pageRequest) {
        return executeQuery(
                firestore.collection("doctors")
                        .whereEqualTo("doctorName", doctorName),
                pageRequest
        );
    }


    public List<DoctorDetails> findAll(PageRequest pageRequest) {
        return executeQuery(
                firestore.collection("doctors"),
                pageRequest
        );
    }

    private List<DoctorDetails> executeQuery(Query query, PageRequest pageRequest) {
        List<DoctorDetails> doctors = new ArrayList<>();

        try {
            ApiFuture<QuerySnapshot> future = query
                    .limit(pageRequest.getPageSize())
                    .offset((int) pageRequest.getOffset())
                    .get();

            QuerySnapshot querySnapshot = future.get();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                DoctorDetails doctor = document.toObject(DoctorDetails.class);
                if (doctor != null) {
                    doctors.add(doctor);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        return doctors;
    }
}
