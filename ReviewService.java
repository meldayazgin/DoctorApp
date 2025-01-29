package com.example.flight.services;

import com.example.flight.models.Review;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;

    public ReviewService() {
        FirebaseApp doctorApp = FirebaseApp.getInstance("doctorapp");
        this.firestore = FirestoreClient.getFirestore(doctorApp);


        this.firebaseAuth = FirebaseAuth.getInstance(doctorApp);
    }

    public void submitReview(Review review, String token) throws Exception {

        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
        String userEmail = decodedToken.getEmail();


        if (!review.getPatientEmail().equals(userEmail)) {
            throw new Exception("Unauthorized: You can only submit reviews for your own appointments.");
        }


        firestore.collection("reviews").add(review);
    }
}
