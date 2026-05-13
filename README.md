<div align="center">
  <h1>🌍 Rehletna</h1>
  <p><strong>Enterprise-Grade Travel Management & Community Platform</strong></p>
  
  [![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
  [![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue.svg)](https://openjfx.io/)
  [![MySQL](https://img.shields.io/badge/MySQL-Database-lightgrey.svg)](https://www.mysql.com/)
  [![Maven](https://img.shields.io/badge/Maven-Build-green.svg)](https://maven.apache.org/)
</div>

<br />

## 📖 Overview

**Rehletna** is a comprehensive desktop application designed to bridge the gap between travel agencies, local guides, and tourists. Developed as part of the *Projet d'Intégration (PI)*, it provides a centralized ecosystem for discovering, booking, and sharing travel experiences. The platform elegantly integrates robust e-commerce capabilities with engaging social features to ensure a complete user journey from trip planning to post-travel community engagement.

---

## ✨ Core Modules

Our modular approach caters to multiple user personas (Administrators, Agencies, Guides, and Clients):

*   🔐 **Identity & Access Management:** Secure user authentication, role-based access control, and comprehensive profile management.
*   🛒 **Booking & Reservations Engine:** End-to-end reservation system for flights and accommodations, including smart cart management and secure payment processing.
*   🏨 **Tourism Services Management:** A dedicated B2B portal for agencies to manage their catalogs (hotels, flights, customized offers) equipped with integrated analytics and revenue tracking.
*   🗺️ **Activities & Experiences:** Intuitive tools for local guides to publish and manage activities, accompanied by AI-assisted scheduling and map integration.
*   🌐 **Social & Community:** A vibrant community hub featuring user publications, a robust moderation workflow, interactive commenting, and AI-powered image analysis for dynamic destination suggestions.
*   💬 **Real-time Messaging:** Integrated peer-to-peer messaging and conversational AI support.
*   🛍️ **Storefront (Marketplace):** Integrated e-commerce module for travel gear and merchandise.

---

## 🏗️ Technical Architecture

Rehletna enforces a strict **Model-View-Controller (MVC)** architecture and Clean Architecture principles to ensure scalability and maintainability:

*   **Entities:** Strongly-typed domain models accurately representing the relational database schema.
*   **Services:** Standardized service layers implementing a generic `CRUD<T>` interface. Business logic is securely handled, heavily utilizing `PreparedStatement` to mitigate SQL injection vulnerabilities.
*   **Controllers:** JavaFX controllers meticulously manage UI state. Heavy computations (such as AI inference and network requests) are offloaded to background threads, leveraging `Platform.runLater()` to ensure a fluid user experience.

---

## 🛠️ Technology Stack

*   **Language:** Java 17
*   **User Interface:** JavaFX with FXML and custom CSS styling
*   **Database:** MySQL (JDBC)
*   **Build Tool:** Maven
*   **Integrations:** iTextPDF (Reporting), Stripe API (Payments), Custom AI Classification Models.

---

## 🚀 Getting Started

Follow these steps to set up the project locally:

1.  **Database Configuration:** 
    Ensure your local MySQL instance is running. Update the database credentials located in `src/main/java/utils/MyDBConnexion.java`.
2.  **Build the Project:** 
    Use Maven to fetch dependencies and compile the project:
    ```bash
    mvn clean compile
    ```
3.  **Run:** 
    Launch the application via the main JavaFX entry point class.

---
<div align="center">
  <small><em>Proprietary and Confidential. Designed for academic evaluation.</em></small>
</div>
