import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom"; 
import { auth } from "../firebase-config"; 
import axios from "axios";
import "./ReviewForm.css";

const ReviewForm = () => {
  const [appointmentId, setAppointmentId] = useState("");
  const [reviewText, setReviewText] = useState("");
  const [rating, setRating] = useState(0);
  const [appointments, setAppointments] = useState([]);
  const [message, setMessage] = useState("");
  const navigate = useNavigate(); 
  useEffect(() => {
    const checkAuth = () => {
      console.log("Checking authentication..."); 
      const unsubscribe = auth.onAuthStateChanged((user) => {
        console.log("Auth state changed:", user); 
        if (!user) {
          alert("You must log in to access this page.");
          console.log("No user detected. Redirecting to login."); 
          navigate("/login"); 
        } else {
          console.log("User is logged in:", user.email);
        }
      });

      return () => unsubscribe(); 
    };

    checkAuth();
  }, [navigate]);

  const fetchAppointments = async () => {
    try {
      console.log("Fetching appointments..."); 
      const user = auth.currentUser;
      console.log("Current user:", user); 

      if (!user) {
        console.log("No user logged in. Skipping fetch."); 
        return;
      }

      const token = await user.getIdToken();
      console.log("Fetched Firebase token:", token); 

      const response = await axios.get("http://localhost:8080/api/doctors/tempAppointments", {
        headers: { Authorization: `Bearer ${token}` },
      });

      console.log("Fetched appointments:", response.data);
      setAppointments(response.data || []);
    } catch (error) {
      console.error("Error fetching appointments:", error); 
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!appointmentId || !reviewText || rating === 0) {
        alert("Please fill in all fields.");
        return;
    }

    try {
        const user = auth.currentUser;

        if (!user) {
            alert("You must log in to submit a review.");
            navigate("/login");
            return;
        }

        const token = await user.getIdToken();

        const selectedAppointment = appointments.find(appt => appt.id === appointmentId);
        if (!selectedAppointment) {
            alert("Invalid appointment selected.");
            return;
        }

        const reviewData = {
            appointmentId,
            reviewText,
            rating,
            patientEmail: user.email,
            doctorName: selectedAppointment.doctorName, 
        };

        console.log("Review data being sent:", reviewData);

        const response = await axios.post(
            "http://localhost:8080/api/reviews",
            reviewData,
            { headers: { Authorization: `Bearer ${token}` } }
        );

        if (response.status === 200) {
            setMessage("Review submitted successfully!");
            setAppointmentId("");
            setReviewText("");
            setRating(0);
        } else {
            setMessage("Failed to submit review. Please try again.");
        }
    } catch (error) {
        console.error("Error submitting review:", error);
        setMessage("Error submitting review. Please try again.");
    }
};


  const handleRatingClick = (value) => {
    console.log("Rating selected:", value); 
    setRating(value);
  };

  useEffect(() => {
    fetchAppointments();
  }, []);

  return (
    <div className="review-form-container">
      <h2>Submit a Review</h2>

      <form onSubmit={handleSubmit} className="review-form">
        <label htmlFor="appointmentId">Select Appointment:</label>
        <select
          id="appointmentId"
          value={appointmentId}
          onChange={(e) => setAppointmentId(e.target.value)}
        >
          <option value="">-- Select an Appointment --</option>
          {appointments.map((appointment) => (
            <option key={appointment.id} value={appointment.id}>
              {appointment.doctorName} - {appointment.day} at {appointment.hour}
            </option>
          ))}
        </select>

        <label htmlFor="reviewText">Review:</label>
        <textarea
          id="reviewText"
          value={reviewText}
          onChange={(e) => setReviewText(e.target.value)}
          placeholder="Write your review here..."
        ></textarea>

        <label htmlFor="rating">Rating:</label>
        <div className="star-rating">
          {[1, 2, 3, 4, 5].map((value) => (
            <span
              key={value}
              className={`star ${rating >= value ? "selected" : ""}`}
              onClick={() => handleRatingClick(value)}
            >
              ★
            </span>
          ))}
        </div>

        <button type="submit" className="submit-button">
          Submit Review
        </button>
      </form>

      {message && <p className="message">{message}</p>}
    </div>
  );
};

export default ReviewForm;
