import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { auth, googleProvider } from "../firebase-config";
import { signInWithPopup } from "firebase/auth";
import axios from "axios";
import "./Signup.css";

const Signup = () => {
    const { userType } = useParams();
    const [formData, setFormData] = useState({
        doctorName: "",
        areaOfInterest: "",
        availableDays: "",
        availableHours: "",
        address: "",
        city: "",
    });
    const [message, setMessage] = useState("");
    const [isStepTwo, setIsStepTwo] = useState(false); 
    const [token, setToken] = useState(""); 

    const registerWithGoogle = async () => {
        try {
            const result = await signInWithPopup(auth, googleProvider);
            const googleToken = await result.user.getIdToken();
            setToken(googleToken); 

            console.log("Google token acquired:", googleToken);

            if (userType === "doctor") {
                console.log("Doctor Register");


                const startResponse = await axios.post(
                    "http://localhost:8080/auth/register/doctor/start",
                    { token: googleToken },
                    {
                        headers: {
                            "Content-Type": "application/json",
                            Authorization: `Bearer ${googleToken}`,
                        },
                    }
                );

                console.log("Start Response:", startResponse);

                if (startResponse.status === 200) {
                    console.log("Google authentication successful:", startResponse.data);
                    setIsStepTwo(true); 
                } else {
                    setMessage("Failed to start registration. Please try again.");
                }
            } else if (userType === "patient") {
                console.log("Patient Register");
                const startResponse = await axios.post(
                    "http://localhost:8080/auth/register/patient",
                    { token: googleToken },
                    {
                        headers: {
                            "Content-Type": "application/json",
                            Authorization: `Bearer ${googleToken}`,
                        },
                    }
                );
                console.log("Patient Registration Response:", startResponse);
                setMessage("Registration successful!");
            } else {
                setMessage("Google authentication failed.");
            }
        } catch (error) {
            console.error("Error registering:", error);
            setMessage("Failed to register. Please try again.");
        }
    };

    const completeDoctorRegistration = async () => {
        try {
            console.log("Form Data Sent to Backend (Complete):", formData);

            const completeResponse = await axios.post(
                "http://localhost:8080/auth/register/doctor/complete",
                {
                    token,
                    doctorName: formData.doctorName,
                    areaOfInterest: formData.areaOfInterest,
                    availableDays: formData.availableDays,
                    availableHours: formData.availableHours,
                    address: formData.address,
                    city: formData.city,
                },
                {
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            console.log("Complete Response:", completeResponse);

            if (completeResponse.status === 200) {
                setMessage("Doctor registration completed successfully!");
                setIsStepTwo(false);
            } else {
                setMessage("Failed to complete registration. Please try again.");
            }
        } catch (error) {
            console.error("Error completing registration:", error);
            setMessage("Failed to complete registration. Please try again.");
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
        console.log("Updated formData:", { ...formData, [name]: value });
    };

    return (
        <div className="signup-container">
            <h2>Register as {userType}</h2>

            {!isStepTwo ? (
                <button type="button" onClick={registerWithGoogle} className="google-button">
                    Register with Google
                </button>
            ) : (
                <form>
                    <input
                        type="text"
                        name="doctorName"
                        placeholder="Full Name"
                        value={formData.doctorName}
                        onChange={handleChange}
                    />
                    <input
                        type="text"
                        name="areaOfInterest"
                        placeholder="Area of Interest"
                        value={formData.areaOfInterest}
                        onChange={handleChange}
                    />
                    <input
                        type="text"
                        name="availableDays"
                        placeholder="Available Days (comma separated)"
                        value={formData.availableDays}
                        onChange={handleChange}
                    />
                    <input
                        type="text"
                        name="availableHours"
                        placeholder="Available Hours (comma separated)"
                        value={formData.availableHours}
                        onChange={handleChange}
                    />
                    <input
                        type="text"
                        name="address"
                        placeholder="Address"
                        value={formData.address}
                        onChange={handleChange}
                    />
                    <input
                        type="text"
                        name="city"
                        placeholder="City"
                        value={formData.city}
                        onChange={handleChange}
                    />
                    <button
                        type="button"
                        onClick={completeDoctorRegistration}
                        className="submit-button"
                    >
                        Complete Registration
                    </button>
                </form>
            )}
            {message && <p>{message}</p>}
        </div>
    );
};

export default Signup;
