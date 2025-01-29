import React, { useState, useEffect } from "react";
import axios from "axios";
import "./DoctorSearch.css";

const AdminSearch = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [city, setCity] = useState("");
  const [doctorName, setDoctorName] = useState(""); 
  const [doctors, setDoctors] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(3);
  const [totalPages, setTotalPages] = useState(1);

  const fetchDoctors = async () => {
    if (!searchTerm.trim() && !city.trim() && !doctorName.trim()) {
      setMessage("Please enter a search term, city, or doctor name.");
      setDoctors([]);
      return;
    }
    setLoading(true);
    setMessage("");

    try {
      const params = { searchTerm, city, doctorName, page, size }; 
      const response = await axios.get("http://localhost:8080/api/doctors/search", { params });
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

  const approveDoctor = async (doctorEmail) => {
    console.log("Attempting to approve doctor with email:", doctorEmail);
    if (!doctorEmail) {
      console.error("No doctor email provided");
      return;
    }

    try {
      const response = await axios.post(
        `http://localhost:8080/api/doctors/approve/${doctorEmail}`,
        {}
      );
      console.log("API Response for approving doctor:", response.data);
      setMessage("Doctor has been approved.");
    } catch (error) {
      console.error("Error approving doctor:", error);
      setMessage("Error occurred while approving the doctor.");
    }
  };

  useEffect(() => {
    fetchDoctors(); 
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
              <p><strong>Specialty:</strong> {doctor.areaOfInterest}</p>
              <p><strong>City:</strong> {doctor.city}</p>
              <p><strong>Address:</strong> {doctor.address}</p>
              <p><strong>Email:</strong> {doctor.email}</p>

              <button
                className="approve-button"
                onClick={() => {
                  console.log("Doctor object:", doctor);  
                  approveDoctor(doctor.email); 
                }}
              >
                Approve Doctor
              </button>
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

export default AdminSearch;
