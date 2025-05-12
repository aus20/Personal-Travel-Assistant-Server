
Postman (GET Request)
        ↓
Spring Boot Dispatcher
        ↓
FlightController.searchFlights(origin, destination)
        ↓
FlightService.searchFlights(origin, destination)
        ↓
Creates List<Flight> (dummy data)
        ↓
Return List<Flight> to Controller
        ↓
Controller returns JSON Response
        ↓
Postman shows the flight list


Later, the flow will look like:

Controller --> Service --> Repository --> Database

With APP side connection:

[ Android App (Retrofit HTTP GET) ]
        ↓
[ Spring Boot Backend Controller ]
        ↓
[ FlightService ]
        ↓
[ (Optional) Repository/Database or Amadeus API ]
        ↓
[ JSON Response to Android ]