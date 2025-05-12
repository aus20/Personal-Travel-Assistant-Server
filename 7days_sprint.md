📆 Sprint 1 — Full Expanded Plan (7 Days)
🛠 Day 1 — Project Setup + Basic Flight Search
Goal: Have server running + simple Flight search API ready.
✅ Create folder structure under com.aus20
✅ Create PersonalTravelAssistantBackendApplication.kt (main app entry)
✅ Create Flight.kt (Entity: id, origin, destination, price)
✅ Create FlightRepository.kt
✅ Create FlightService.kt (basic service methods)
✅ Create FlightController.kt (/api/flights)
✅ Endpoint: GET /api/flights/search?origin=XXX&destination=YYY ✅ Mock flight search temporarily (hardcoded data, not Amadeus yet)
✅ Test with Postman: make sure /api/flights/search returns mock flights

🛠 Day 2 — User Registration, Login, Logout
Goal: Basic user authentication working.
✅ Create User.kt (Entity: id, email, password (plain text for now), name)
✅ Create UserRepository.kt
✅ Create DTOs: UserRegisterDTO, UserLoginDTO, UserResponseDTO
✅ Create UserService.kt (registerUser, loginUser)
✅ Create UserController.kt
✅ Endpoints:

POST /api/users/register

POST /api/users/login

POST /api/users/logout (fake logout — just clear session on client)

✅ Simple login logic (no JWT, keep it simple)
✅ Test registration and login via Postman.

🛠 Day 3 — Connect Real Amadeus API for Flight Search
Goal: Real flight data integration.
✅ Setup Amadeus API credentials in application.yml
✅ Create AmadeusConfig.kt to read keys
✅ Create AmadeusService.kt

Authenticate with Amadeus

Search flights API (with origin, destination)

✅ Update FlightService.kt to call AmadeusService
✅ Update FlightController.kt to fetch real Amadeus flights instead of mock
✅ Test real flight search with Postman

🛠 Day 4 — Save Flight Searches for Future Tracking
Goal: Allow users to track a search.
✅ Create FlightTracking.kt (Entity: id, user_id, origin, destination, date, last_known_price)
✅ Create FlightTrackingRepository.kt
✅ Create DTOs: FlightTrackingRequestDTO, FlightTrackingResponseDTO
✅ Create FlightTrackingService.kt

saveTracking

listTracking

updateTracking

deleteTracking

✅ Create FlightTrackingController.kt

POST /api/tracks

GET /api/tracks

PUT /api/tracks/{id}

DELETE /api/tracks/{id}

✅ Test full CRUD on tracking saved flights.

🛠 Day 5 — Background Flight Search + Notifications
Goal: Auto-check prices, notify if cheaper.
✅ Add @Scheduled task inside FlightTrackingService.kt

Every 6 hours: re-search saved flights via Amadeus API

Compare current price vs. last_known_price

If cheaper, save notification or log to console

✅ (Optional) Create Notification.kt (Entity: id, user_id, message, created_at)
✅ (Optional) Create NotificationRepository.kt and NotificationService.kt

✅ Test that the background job runs and logs notifications.

🛠 Day 6 — Input Validation + Error Handling
Goal: Make APIs clean, safe, and professional.
✅ Add validation annotations to DTOs:

@NotBlank

@Email

@Min

@Max

etc.

✅ Create GlobalExceptionHandler.kt

Handle 400 Bad Requests

Handle 404 Not Found

Handle 500 Internal Errors

✅ Clean up API responses (use ResponseEntity properly)

✅ Improve error messages for wrong inputs.

🛠 Day 7 — Full Testing, Final Polish, Documentation
Goal: Make sure everything is smooth and ready to present.
✅ Full Postman testing for all APIs:

Registration

Login

Search flights

Save flight search

Update/delete tracking

Check price drop

✅ Minor bug fixes ✅ Add small README.md file explaining:

How to run backend

List of APIs

Sample JSON for requests

✅ Final database check (tables created properly, prices updating)


🎯 Sprint 1 Master Checklist (Big View)

1	Setup project + basic flight API (mock)	x
2	User auth (register/login/logout)	x
3	Connect Amadeus API for real flights	x
4	Save/Search/Update/Delete tracking	⬜
5	Background price checks + notification	⬜

6	Validation + error handling polish	⬜
7	Full testing + final polish	⬜

YAPILMASI GEREKENLER:
Flight Search mechanism koyulması
Database’e uçuşların kaydedilmesi (mesela en ucuz ilk 10 uçuş)(data caching)
User bir flight search yaptığında ilk önce database
User’ın kaydettiği uçuşların database’e kaydedilmesi (User JWT User’ın kendi uçuşları olacak)
Kaydedilen uçuşların Flight tracking’inin yapılması (notification + periodic search)
Daha uygun uçuş bulunması durumunda bildirim verilmesi (yine notificaion + periodic search)
Kaydedilen Uçuşların Save/Search/Update/Delete (GENEL)

Havaalanın’dan ülke bilgisine dönüştürme
Carrier Transformation TK to turksih airlines
USD to TL transformation (çok gerekli değil)


Sprint 2: Server Improvements and Real Features

⭐ 1	Add JWT Authentication	Protect your future APIs (flights, trackings). Issue token on login, validate token on requests. (Finished.)
⭐ 2	Connect to Amadeus API	Setup Amadeus API credentials. Create a service to search real flights, instead of mock. (finished)
⭐ 5	Implement Scheduled Background Jobs	Use @Scheduled tasks to check flight prices periodically. Send notification (later).
⭐ 3	Implement Tracking Model + CRUD APIs	Entity TrackedFlight, linked to a User. Create/save/update/delete endpoints.
⭐ 4	Add Global Exception Handler	Create a @ControllerAdvice to return clean error responses for 400/401/404/500.

⭐ 6	Apply Request Validation	Use @Valid and DTO annotations (@NotNull, @Email, @Size, etc.) to enforce input rules.
⭐ 7	Add Basic Integration Tests	Test Register/Login/Flight Search/Tracking CRUD endpoints with MockMvc or RestAssured.
