🛡 REGISTER Flow:
1.User sends a POST request to /api/users/register:

Example JSON:

{
  "email": "test@example.com",
  "name": "John Doe",
  "password": "mysecretpassword"
}

2.Spring Controller (UserController) catches it:


@PostMapping("/register")
fun register(@RequestBody registerDTO: UserRegisterDTO): ResponseEntity<UserResponseDTO>
3.UserService.registerUser() is called:

It takes the plain password from the request (dto.password).

Uses BCryptPasswordEncoder to hash it securely:

val hashedPassword = passwordEncoder.encode(dto.password)
It creates a User entity with:
email
hashed password
name

4.User is saved to the database:

val savedUser = userRepository.save(user)

5. Response:

Server sends back a safe UserResponseDTO (without password) like:

{
  "id": 1,
  "email": "test@example.com",
  "name": "John Doe"
}

🔒 LOGIN Flow:
1. User sends a POST request to /api/users/login:

Example JSON:

{
  "email": "test@example.com",
  "password": "mysecretpassword"
}

2. Spring Controller (UserController) catches it:

@PostMapping("/login")
fun login(@RequestBody loginDTO: UserLoginDTO): ResponseEntity<UserResponseDTO>

3. UserService.loginUser() is called:

It looks up the user by email:

val user = userRepository.findByEmail(dto.email) ?: return null

4. Password Verification:

It checks if the hashed password stored in database matches the plain password provided:

if (passwordEncoder.matches(dto.password, user.password))

BCryptPasswordEncoder.matches() internally re-hashes the input password and compares it to the stored hash securely.

5. If correct:

    Returns the safe UserResponseDTO (no password).

6. If wrong:

    Returns 400 Bad Request.

