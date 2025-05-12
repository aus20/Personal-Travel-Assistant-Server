🧩 Full JWT Authentication Flow (Register & Login)

1️⃣ User Registration (/api/users/register)

When a new user registers:
📲 App sends a POST to /api/users/register with:

{
  "email": "newuser@example.com",
  "password": "password123",
  "name": "New User"
}
🔥 Controller (UserController) receives the request and calls:
userService.registerUser(registerDTO)

🧠 Service (UserService) does:

    Encodes the password using BCryptPasswordEncoder.

    Creates and saves a new User entity in the database (UserRepository).

✅ Response: Returns a UserResponseDTO (id, email, name).

No JWT issued yet — just registration complete.


2️⃣ User Login (/api/users/login)

When a user logs in:
📲 App sends a POST to /api/users/login with:

{
  "email": "newuser@example.com",
  "password": "password123"
}

🔥 Controller (UserController) calls:

userService.loginUser(loginDTO)
🧠 Service (UserService) does:

Looks up the user by email using UserRepository.

Verifies the entered password matches the hashed password with:
passwordEncoder.matches(plainPassword, hashedPassword)

If valid, it generates a JWT Token using JwtTokenProvider.generateToken(user.email).

✅ Response: Sends back a JwtLoginResponse:

{
  "user": {
    "id": 1,
    "email": "newuser@example.com",
    "name": "New User"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." 
}
✅ Now the app saves the token locally (SharedPreferences, EncryptedStorage, etc.).