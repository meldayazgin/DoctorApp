
package com.example.flight.controllers;

import com.example.flight.models.Review;
import com.example.flight.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<String> submitReview(@RequestBody Review review, @RequestHeader("Authorization") String authHeader) {
        try {

            String token = authHeader.replace("Bearer ", "");


            reviewService.submitReview(review, token);

            return ResponseEntity.ok("Review submitted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit review.");
        }
    }
}
