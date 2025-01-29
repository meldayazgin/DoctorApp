# DoctorApp
Doctor Appointment System

Our Doctor Appointment System is designed as a platform that enables patients to search for doctors, schedule appointments, and provide feedback, while allowing doctors to manage their availability and receive comments and ratings. The system follows a microservices architecture for and ease of maintenance. Authentication is handled through Firebase Authentication with Google, ensuring secure access for both doctors and patients.

Core System Components:

Frontend: A responsive UI built with React.

Backend Services: Built with Java and Spring Boot, ensuring modular and scalable operations.

Database: Firestore, a NoSQL cloud database, for storing patient comments, appointments, and user data.

Notifications: A dedicated notification service sends email reminders for incomplete appointments and post-visit reviews.

Scheduling: An asynchronous scheduling mechanism manages daily notifications and appointment follow-ups.

Message Queues: RabbitMQ is used for handling asynchronous messaging and improving system reliability.

Assumptions

API Gateway: Assumed to be in place for managing service communication, which some are independent already.

Cloud-Based Scheduling: Assumed to be managed via cloud services, but is currently fully running through an internal scheduling mechanism.

Map Functionality: Address data is stored in the system and assumed to be functional for future geolocation mapping via a simple API file integration.

Temporary Local Hosting: The project is currently hosted locally since Microsoft Azure is costly. As discussed in the previous project because of PostGre SQL usage, my available credits were low.

Challenges and Solutions

Service Communication: Some services are independent and assumed to be connected through the API Gateway.

Efficient Scheduling: Instead of cloud-based scheduling services, an internal asynchronous scheduler was implemented to manage reminders efficiently.

Overall, the system successfully meets all the core requirements, leveraging scalable architecture, with potential enhancements in caching, filtering, and cloud-based automation.

Note: Project codes are uploaded in Google Drive because of their file size. Same folder contains teh presentation video, an ER diagram file with picture and both RabbitMQ queue screenshots. Backend codes are titled flight and frontend code is titled taskmanager-app. 
