import React, { useState, useEffect } from "react";
import { auth } from "../firebase-config";
import axios from "axios";
import "./DoctorSearch.css";

const DoctorSearch = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [city, setCity] = useState("");
  const [doctorName, setDoctorName] = useState(""); 
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(3);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedAppointment, setSelectedAppointment] = useState(null);

  const fetchDoctors = async () => {
    if (!searchTerm.trim() && !city.trim() && !doctorName.trim()) {
      setMessage("Please enter a search term, city, or doctor name.");
      setDoctors([]);
      return;
    }
    setLoading(true);
    setMessage("");
    console.log("Doctor Name:", doctorName);

    try {
      const params = { searchTerm, city, doctorName, page, size }; 
      const response = await axios.get("http://localhost:8080/api/doctors/search", { params });
      console.log("Backend Response:", response.data);
      console.log("API Response Data:", response.data);

      if (response.status === 200) {
        const { doctors, totalPages } = response.data;
        setDoctors(doctors || []);
        setTotalPages(totalPages || 1);
        setMessage(doctors.length === 0 ? "No results found." : "");
      } else {
        setMessage("No results found.");
        setDoctors([]);
      }
    } catch (error) {
      console.error("Error fetching doctors:", error);
      setMessage("Failed to fetch results. Please try again.");
      setDoctors([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchAppointments = async () => {
    try {
      const user = auth.currentUser;
      if (!user) return;

      const token = await user.getIdToken();
      const response = await axios.get("http://localhost:8080/api/doctors/tempAppointments", {
        headers: { Authorization: `Bearer ${token}` },
      });

      setAppointments(response.data || []);
    } catch (error) {
      console.error("Error fetching appointments:", error);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setPage(1);
    fetchDoctors();
  };

  const handleSearchTermChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleCityChange = (e) => {
    setCity(e.target.value);
  };

  const handleDoctorNameChange = (e) => {  
    setDoctorName(e.target.value);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  const handleAppointment = async (doctor, hour, day) => {
    try {
        const confirm = window.confirm(
            `Do you want to book an appointment with Dr. ${doctor.doctorName} at ${hour} on ${day}?`
        );

        if (!confirm) {
            return; 
        }

        const user = auth.currentUser;
        if (!user) {
            alert("Please log in to book an appointment.");
            window.location.href = "/login/patient"; 
            return;
        }

        const patientEmail = user.email;
        const patientName = user.displayName || "Unknown Patient";

        const appointmentData = {
            areaOfInterest: doctor.areaOfInterest,
            day,
            doctorEmail: doctor.email,
            doctorName: doctor.doctorName,
            hour,
            patientEmail,
            patientName,
        };

        console.log("Saving Appointment Data:", appointmentData);

        const response = await axios.post("http://localhost:8080/api/doctors/tempAppointments", appointmentData);

        if (response.status === 200) {
            alert("Temporary appointment saved successfully. Redirecting to login...");
            window.location.href = "/login/patient"; 
        } else {
            console.error("Failed to save temporary appointment:", response.data);
        }
    } catch (error) {
        console.error("Error saving temporary appointment:", error);
    }
};

  const renderAvailabilityButtons = (doctor) => {
    const days = doctor.availableDays?.split(",").map((day) => day.trim()) || [];
    const hours = doctor.availableHours?.split(",").map((hour) => hour.trim()) || [];
    const confirmedAppointments = appointments 
      .filter((appt) => appt.doctorEmail === doctor.email && appt.status === "Confirmed")
      .map((appt) => ({ hour: appt.hour, day: appt.day }));

    return (
      <div className="availability-container">
        <div className="days-container">
          <h5>Available Days:</h5>
          <div className="availability-buttons">
            {days.map((day, index) => (
              <div key={`day-${index}`} className="day-column">
                <button
                  className="day-button"
                  onClick={() =>
                    setSelectedAppointment((prev) => ({
                      ...prev,
                      day: day,
                      doctorName: doctor.doctorName,
                      doctorEmail: doctor.email,
                    }))
                  }
                >
                  {day}
                </button>
                {selectedAppointment?.day === day &&
                  selectedAppointment?.doctorEmail === doctor.email && (
                    <div className="hours-buttons">
                      {hours.map((hour, idx) => {
                        const isBooked = confirmedAppointments.some(
                          (appt) => appt.hour === hour && appt.day === day
                        );

                        return (
                          <button
                            key={`hour-${idx}`}
                            className={`hour-button ${isBooked ? "disabled" : ""}`}
                            onClick={() => {
                              if (isBooked) {
                                alert("This appointment is already booked.");
                              } else {
                                handleAppointment(doctor, hour, day);
                              }
                            }}
                            disabled={isBooked}
                          >
                            {hour}
                          </button>
                        );
                      })}
                    </div>
                  )}
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  };

  useEffect(() => {
    fetchAppointments(); 
    if (searchTerm || city || doctorName) { 
      fetchDoctors();
    }
  }, [page, searchTerm, city, doctorName]); 

  return (
    <div className="doctor-search-container">
      <h2>Search for Doctors</h2>
      <form onSubmit={handleSubmit} className="search-form">
        <input
          type="text"
          name="searchTerm"
          placeholder="Search by name or specialty"
          value={searchTerm}
          onChange={handleSearchTermChange}
        />
        <input
          type="text"
          name="city"
          placeholder="Search by city"
          value={city}
          onChange={handleCityChange}
        />
        <input
          type="text"
          name="doctorName" 
          placeholder="Search by doctor name"
          value={doctorName}
          onChange={handleDoctorNameChange}
        />
        <button type="submit" className="search-button">
          {loading ? "Searching..." : "Search"}
        </button>
      </form>

      {message && <p className="message">{message}</p>}

      {!loading && Array.isArray(doctors) && doctors.length > 0 && (
        <div className="doctors-list">
          {doctors.map((doctor, index) => (
            <div key={index} className="doctor-card">
              <h3>{doctor.doctorName}</h3>
              <p>
                <strong>Specialty:</strong> {doctor.areaOfInterest}
              </p>
              <p>
                <strong>City:</strong> {doctor.city}
              </p>
              <p>
                <strong>Address:</strong> {doctor.address}
              </p>
              <p>
                <strong>Email:</strong> {doctor.email}
              </p>
              
              <div>{renderAvailabilityButtons(doctor)}</div>
            </div>
          ))}
        </div>
      )}

      {!loading && Array.isArray(doctors) && doctors.length === 0 && !message && (
        <p>No doctors found. Please try a different search.</p>
      )}

      <div className="pagination-container">
        {Array.from({ length: totalPages }, (_, idx) => (
          <button
            key={idx}
            className={`pagination-button ${page === idx + 1 ? "active" : ""}`}
            onClick={() => handlePageChange(idx + 1)}
          >
            {idx + 1}
          </button>
        ))}
      </div>
    </div>
  );
};

export default DoctorSearch;
