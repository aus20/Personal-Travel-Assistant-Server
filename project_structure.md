## Project Structure

```
personal-travel-assistant-backend/
src/
└── main/
    └── kotlin/
        └── com/
            └── aus20/
                ├── config/
                │   ├── AmadeusPropHolder.kt
                │   ├── RestTemplateConfig.kt
                │   └── SecurityConfig.kt
                │
                ├── controller/
                │   ├── FlightController.kt
                │   ├── UserController.kt
                │   └── UserFlightSearchController.kt
                │
                ├── domain/
                │   ├── Flight.kt
                │   ├── User.kt
                │   └── UserFlightSearch.kt
                │
                ├── dto/
                │   ├── request/
                │   │   ├── FlightSearchRequestDTO.kt
                │   │   ├── UserLoginDTO.kt
                │   │   └── UserRegisterDTO.kt
                │   └── response/
                │       ├── FlightResponseDTO.kt
                │       ├── JwtLoginResponse.kt
                │       ├── RoundTripFlightResponseDTO.kt
                │       └── UserResponseDTO.kt
                │
                ├── integration/ (Empty)
                │
                ├── repository/
                │   ├── FlightRepository.kt
                │   ├── UserFlightSearchRepository.kt
                │   └── UserRepository.kt
                │
                ├── security/
                │   ├── CustomUserDetailsService.kt
                │   ├── JwtAuthenticationFilter.kt
                │   └── JwtTokenProvider.kt
                │   └── CurrentUser.kt
                │   └── CurrentUserResolver.kt
                │
                └── service/
                │    ├── AmadeusAuthService.kt
                │    ├── FlightService.kt
                │    ├── UserFlightSearchService.kt
                │    ├── UserService.kt
                └── PersonalTravelAssistantBackendApplication.kt        

        resources/ application.yml
    test/
.gitignore
7days_sprint.md
build.gradle.kts

Proper flow (Clean Architecture MVVM-like):

Controller --> Service --> Repository --> Database

✅ Controller: only handles API requests/responses.
✅ Service: handles all business logic (even simple operations).
✅ Repository: just fetches/saves entities to database.


Android App --> HTTP Request --> FlightController --> FlightService--> FlightRepository (if local DB needed)
OR
--> AmadeusService (if external search needed)
--> Return result to Android

🧠 What is Hibernate (JPA)?

JPA is a specification (like a rulebook).

It defines how Java/Kotlin objects should be mapped to database tables.

JPA does not provide code itself — just rules/interfaces.

Hibernate is an actual library that implements the JPA rules.

Hibernate does the real work: connecting to the database, executing SQL, managing tables and records.

Spring Boot uses Hibernate by default under the hood.

✅ Hibernate = The real engine that makes JPA work.
