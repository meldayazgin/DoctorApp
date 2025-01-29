import React from 'react';
import { Link } from 'react-router-dom';
import './navbar.css';

const Navbar = () => {
  return (
    <nav className="navbar">
      <h2>Doctor Appointment System</h2>
      <ul className="navbar-menu">
        <li className="navbar-item">
          <Link to="/signup/doctor">Register as a Doctor</Link> 
        </li>
        <li className="navbar-item">
          <Link to="/signup/patient">Register as a Patient</Link> 
        </li>
        <li className="navbar-item">
          <Link to="/login/patient">Login</Link> 
        </li>
        <li className="navbar-item">
          <Link to="/doctor-search">Search for Doctors</Link> 
        </li>
        <li className="navbar-item">
          <Link to="/review-form">Review</Link> 
        </li>
        <li className="navbar-item">
        <Link to="/admin-dashboard">Admin Login</Link> 
        </li>
      </ul>
    </nav>
  );
};

export default Navbar;
