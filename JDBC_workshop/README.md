# Rehletna Desktop Application

Rehletna is a comprehensive, enterprise-grade travel management and community platform. Designed to bridge the gap between travel agencies, local guides, and tourists, the application provides a centralized ecosystem for discovering, booking, and sharing travel experiences.

## System Overview

The platform is structured modularly, catering to multiple user personas including Administrators, Agencies, Guides, and Clients. It integrates core e-commerce capabilities with social features, ensuring a complete user journey from trip planning to post-travel community engagement.

## Core Modules

*   **Identity & Access Management:** Secure user authentication, role-based access control (Admin, Agency, Guide, Client), and profile management.
*   **Booking & Reservations Engine:** End-to-end reservation system for flights and accommodations, including cart management and secure payment processing.
*   **Tourism Services Management:** A dedicated portal for agencies to manage their catalogs (hotels, flights, customized offers) with integrated analytics and revenue tracking.
*   **Activities & Experiences:** Tools for local guides to publish and manage activities, accompanied by AI-assisted scheduling and map integration.
*   **Social & Community:** A community hub featuring user publications, a moderation workflow, interactive commenting, and AI-powered image analysis for destination suggestions.
*   **Real-time Messaging:** Integrated peer-to-peer messaging and conversational AI support.
*   **Storefront (Marketplace):** E-commerce module for travel gear and merchandise.

## Technical Stack

*   **Language:** Java 17
*   **User Interface:** JavaFX with FXML and custom CSS styling
*   **Database:** MySQL (JDBC)
*   **Build Tool:** Maven
*   **Integrations:** iTextPDF (Reporting), Stripe (Payments), and custom AI Classification Models.

## Architecture

The project enforces a strict **Model-View-Controller (MVC)** architecture, emphasizing separation of concerns:
*   **Entities:** Strongly-typed domain models representing the database schema.
*   **Services:** Standardized service layers implementing a generic `CRUD<T>` interface to handle business logic securely (utilizing `PreparedStatement`).
*   **Controllers:** JavaFX controllers managing UI state, offloading heavy computations (e.g., AI inference, network requests) to background threads via `Platform.runLater()`.

## Getting Started

1.  **Database Configuration:** Update the database credentials in `utils.MyDBConnexion`.
2.  **Build:** Execute `mvn clean compile` to fetch dependencies and compile the project.
3.  **Run:** Launch the application via the main JavaFX entry point.

---
*Proprietary and Confidential. Designed for the Projet d'Intégration (PI) evaluation.*
