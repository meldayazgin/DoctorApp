import React from "react";
import { useNavigate } from "react-router-dom";
import "./MainPage.css";

const MainPage = () => {
    const navigate = useNavigate();

    return (
        <div className="main-page">
            <h1>Welcome to the Appointment System</h1>
            <div className="button-group">
                <button onClick={() => navigate("/login/patient")} className="button">
                    Login as Patient
                </button>
                <button onClick={() => navigate("/signup/doctor")} className="button">
                    Register as Doctor
                </button>
                <button onClick={() => navigate("/admin-dashboard")} className="button">
                    Login as Admin
                </button>
                <button onClick={() => navigate("/signup/patient")} className="button">
                    Register as Patient
                </button>
                <button onClick={() => navigate("/doctor-search")} className="button">
                    Search for Doctors
                </button>
                <button onClick={() => navigate("/review-form")} className="button">
                    Review
                </button>
            </div>
        </div>
    );
};

export default MainPage;
