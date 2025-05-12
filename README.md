# Personal Travel Assistant Backend

A Spring Boot backend application for managing travel-related services.

## Project Structure

```
personal-travel-assistant-backend/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/
│   │   │       └── yourname/
│   │   │           ├── config/         # Spring Boot configurations
│   │   │           ├── controller/     # REST controllers
│   │   │           ├── dto/            # Data Transfer Objects
│   │   │           ├── domain/         # Entity classes
│   │   │           ├── repository/     # JPA repositories
│   │   │           ├── service/        # Business logic
│   │   │           └── integration/    # External API integrations
│   │   │           └── PersonalTravelAssistantBackendApplication.kt  # Main app launcher
│   │   └── resources/
│   │       ├── application.yml        # Application configuration
│   │       └── static/                # Static resources
│   │       └── templates/             # (if you ever use server-side rendering, rare here)
│   └── test/                          # Test classes
│       └── kotlin/ (same structure as main, for tests)
├── build.gradle.kts                      # Gradle Kotlin DSL build file
├── .gitignore                             # Git ignore rules
├── README.md    

## Setup

1. Make sure you have Java 17 and Gradle installed
2. Update the database configuration in `application.yml`
3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

## Features

- Flight management
- User authentication and authorization
- Integration with Amadeus API for flight data
- RESTful API endpoints

## Technology Stack

- Kotlin
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Gradle