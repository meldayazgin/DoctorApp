import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import { auth, googleProvider } from "../firebase-config";
import { signInWithPopup } from "firebase/auth";
import axios from "axios";
import "./Login.css";

const Login = () => {
    const { userType } = useParams();
    const [message, setMessage] = useState("");
    const [tempAppointments, setTempAppointments] = useState([]);
    const [isLoading, setIsLoading] = useState(false);  
    const navigate = useNavigate();


    const loginWithGoogle = async () => {
        try {
            const result = await signInWithPopup(auth, googleProvider);
            const token = await result.user.getIdToken();

            const endpoint = userType === "doctor" ? "/auth/login" : "/auth/register/patient";

            const response = await axios.post(
                `http://localhost:8080${endpoint}`,
                { token },
                {
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                }
            );
            setMessage(response.data.message || "Login successful!");

            setIsLoading(true);
            const tempAppointmentsResponse = await axios.get(
                "http://localhost:8080/api/doctors/tempAppointments",
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            setIsLoading(false);

            const appointments = tempAppointmentsResponse.data || [];
            setTempAppointments(appointments);

            if (appointments.length > 0) {
                alert(`You have ${appointments.length} pending appointment(s). Please confirm them.`);
            } else {
                setMessage("No pending appointments found.");
            }
        } catch (error) {
            console.error("Error during login:", error);
            setMessage("Failed to login. Please try again.");
        }
    };

    const handleConfirmAppointment = async (appointment) => {
        try {
            const token = await auth.currentUser.getIdToken();
            const response = await axios.post(
                "http://localhost:8080/api/doctors/appointments",
                appointment,
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
    
            if (response.status === 200) {

                setMessage("Appointment confirmed successfully!");
                setTempAppointments((prev) =>
                    prev.map((appt) =>
                        appt.id === appointment.id
                            ? { ...appt, status: "Confirmed" } 
                            : appt
                    )
                );
            } else {
                setMessage("Failed to confirm the appointment. Please try again.");
            }
        } catch (error) {
            console.error("Error confirming appointment:", error);
            setMessage("Error occurred while confirming the appointment.");
        }
    };
    

    const handleDeleteAppointment = async (appointmentId) => {
        try {
            const token = await auth.currentUser.getIdToken();
            const response = await axios.delete(
                `http://localhost:8080/api/doctors/tempAppointments/${appointmentId}`,
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            if (response.status === 200) {
                setMessage("Appointment deleted successfully!");
                setTempAppointments((prev) =>
                    prev.filter((appt) => appt.id !== appointmentId)
                );
            } else {
                setMessage("Failed to delete the appointment. Please try again.");
            }
        } catch (error) {
            console.error("Error deleting appointment:", error);
            setMessage("Error occurred while deleting the appointment.");
        }
    };

    const handleCompleteVisit = async (appointment) => {
        try {
            const token = await auth.currentUser.getIdToken();
            const response = await axios.post(
                `http://localhost:8080/api/doctors/tempAppointments/${appointment.id}/complete`,
                {},
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            if (response.status === 200) {
                setMessage("Visit marked as completed!");
                setTempAppointments((prev) =>
                    prev.map((appt) =>
                        appt.id === appointment.id ? { ...appt, visitStatus: "Completed" } : appt
                    )
                );
            } else {
                setMessage("Failed to complete the visit. Please try again.");
            }
        } catch (error) {
            console.error("Error completing the visit:", error);
            setMessage("Error occurred while completing the visit.");
        }
    };

    return (
        <div className="login-container">
            <h2>Login as {userType}</h2>
            <button onClick={loginWithGoogle} className="google-button">
                Login with Google
            </button>
            {message && <p>{message}</p>}

            {isLoading ? (
                <p>Loading your appointments...</p>
            ) : (
                <div className="appointment-list">
                    <h3>Pending Appointments</h3>
                    {tempAppointments.length === 0 && <p>No pending appointments found.</p>}

                    {tempAppointments.map((appointment) => (
                        <div key={appointment.id} className="appointment-item">
                            <p>
                                {appointment.doctorName} - {appointment.hour} on {appointment.day}
                            </p>
                            {appointment.status === "Confirmed" && appointment.visitStatus === "Not Completed" && (
                                <button
                                    className="confirm-button"
                                    onClick={() => handleCompleteVisit(appointment)}
                                >
                                    Mark Visit Completed
                                </button>
                            )}
                            {appointment.status === "Confirmed" && appointment.visitStatus === "Completed" && (
                            <button 
                                className="confirm-button" 
                                onClick={() => navigate("/review-form")}
                                style={{ cursor: 'pointer' }}  
                            >
                                Rate Completed Visit
                            </button>
                        )}

 
                            {appointment.status === "Not Confirmed" && (
                                <button
                                    className="confirm-button"
                                    onClick={() => handleConfirmAppointment(appointment)}
                                >
                                    Confirm Appointment
                                </button>
                            )}
                            <button
                                className="delete-button"
                                onClick={() => handleDeleteAppointment(appointment.id)}
                            >
                                Delete Appointment
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default Login;
