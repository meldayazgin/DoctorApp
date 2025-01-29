import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { auth, googleProvider } from "../firebase-config";
import { signInWithPopup } from "firebase/auth";
import axios from "axios";
import "./Login.css";

const AdminDashboard = () => {
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);  
    const navigate = useNavigate(); 

    const loginWithGoogle = async () => {
        try {
            console.log("Starting Google Sign-In...");
            setIsLoading(true);

            const result = await signInWithPopup(auth, googleProvider);
            console.log("Google Sign-In Successful:", result);

            const token = await result.user.getIdToken();
            console.log("Retrieved Firebase Token:", token);


            const response = await axios.post(
                "http://localhost:8080/auth/login", 
                { token },
                {
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            setMessage(response.data.message || "Login successful!");


            navigate("/admin-search");  

        } catch (error) {
            console.error("Error during admin login:", error);
            setMessage("Failed to login. Please try again.");
        } finally {
            setIsLoading(false);
            console.log("Admin login process completed.");
        }
    };

    return (
        <div className="login-container">
            <h2>Login</h2>
            <button onClick={loginWithGoogle} className="google-button">
                {isLoading ? "Logging in..." : "Login with Google"}
            </button>
            {message && <p>{message}</p>}
        </div>
    );
};

export default AdminDashboard;
