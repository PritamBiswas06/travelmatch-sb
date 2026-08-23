# 🌍 Travel Match - Backend

Travel Match is a Spring Boot based backend application that helps users
find compatible travel partners based on destination, budget, travel
dates, and preferences.

------------------------------------------------------------------------

## 🚀 Tech Stack

-   Java 21
-   Spring Boot 3
-   Spring Data JPA
-   Spring Security
-   MySQL
-   Maven
-   Lombok

------------------------------------------------------------------------

## 📌 Project Overview

Travel Match allows users to:

-   Register and login securely
-   Create travel plans
-   Select destination, budget, and travel dates
-   Get matched with compatible travel partners
-   Send and accept match requests

The matching system works based on:

-   Same destination
-   Overlapping travel dates
-   Budget compatibility
-   Age similarity

------------------------------------------------------------------------

## 🏗️ Project Structure

com.pvp.travelmatch │ ├── config ├── controller ├── entity ├──
repository ├── service └── security

------------------------------------------------------------------------

## ⚙️ Setup Instructions

### 1️⃣ Clone Repository

git clone https://github.com/travel-match-team/travelmatch-backend.git

### 2️⃣ Create MySQL Database

CREATE DATABASE travelmatch;

### 3️⃣ Configure application.properties

spring.datasource.url=jdbc:mysql://localhost:3306/travelmatch\
spring.datasource.username=your_username\
spring.datasource.password=your_password

### 4️⃣ Run Application

mvn spring-boot:run

Or run TravelmatchApplication.java from your IDE.

------------------------------------------------------------------------

## 📡 API Endpoints

### Authentication

POST /api/auth/register

(More APIs coming soon)

------------------------------------------------------------------------

## 🧠 Future Enhancements

-   JWT Authentication
-   Travel Plan Entity
-   Advanced Matching Algorithm
-   Match Request System
-   Real-time Chat (WebSocket)
-   Email Notification System
-   Rating & Review Feature

------------------------------------------------------------------------

## 👨‍💻 Team Project

Developed as a collaborative project using GitHub Organization.

Backend: Spring Boot\
Frontend: Angular (In Progress)

------------------------------------------------------------------------

## 📜 License

This project is developed for learning and portfolio purposes.
