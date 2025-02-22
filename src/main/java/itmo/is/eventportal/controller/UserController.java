package itmo.is.eventportal.controller;

import itmo.is.eventportal.entitie.enums.UserRole;
import itmo.is.eventportal.entitie.model.User;
import itmo.is.eventportal.service.UserService;
import itmo.is.eventportal.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/user")
public class UserController {
	private final JwtTokenUtil jwtTokenUtil;
	private final UserService userService;

	@Autowired
	public UserController(JwtTokenUtil jwtTokenUtil, UserService userService) {
		this.jwtTokenUtil = jwtTokenUtil;
		this.userService = userService;
	}

	@PostMapping("/signin")
	public ResponseEntity<?> signinForm(@RequestParam String email, @RequestParam String password) {
		User user = userService.getUserByEmail(email);

		if (user != null && user.getPassword().equals(password)) {
			String jwtToken = jwtTokenUtil.generateJwtToken(user.getEmail());
			return ResponseEntity.ok("{\"token\":\"" + jwtToken + "\"}");
		} else {
			return ResponseEntity.status(401).body("{\"error\":\"Invalid credentials\"}");
		}
	}

	@PostMapping("/signup")
	public ResponseEntity<?> signupForm(@RequestBody User formData) {
		if (formData.getEmail() == null || formData.getEmail().isEmpty()) {
			return ResponseEntity.status(400).body("{\"error\":\"Email не может быть пустым.\"}");
		}

		if (userService.getUserByEmail(formData.getEmail()) != null) {
			return ResponseEntity.status(409).body("{\"error\":\"Email занят. Попробуйте другой.\"}");
		}
		User newUser = new User(formData.getName(), formData.getEmail(), formData.getPassword());
		if (formData.getPhoneNumber() != null) {
			newUser.setPhoneNumber(formData.getPhoneNumber());
		}
		newUser.setRole(UserRole.PARTICIPANT);
		userService.createUser(newUser);

		String jwtToken = jwtTokenUtil.generateJwtToken(newUser.getEmail());
		return ResponseEntity.ok("{\"token\":\"" + jwtToken + "\"}");
	}

	@PutMapping("/update")
	public ResponseEntity<?> updateData(@RequestHeader("Authorization") String token, @RequestBody User formData) {
		String emailFromToken = userService.extractEmail(token);
		User user = userService.getUserByEmail(emailFromToken);
		if (user == null) {
			return ResponseEntity.status(401).body("{\"error\":\"Неверный или просроченный токен\"}");
		}
		if (formData.getName() != null) {
			user.setName(formData.getName());
		}
		if (formData.getEmail() != null) {
			user.setEmail(formData.getEmail());
		}
		if (formData.getPhoneNumber() != null) {
			user.setPhoneNumber(formData.getPhoneNumber());
		}
		if (formData.getPassword() != null) {
			user.setPassword(formData.getPassword());
		}
		userService.updateUser(token, user);
		return ResponseEntity.ok("{\"message\":\"Данные успешно изменены\"}");
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String token) {
		String email = userService.extractEmail(token);
		userService.deleteUserByEmail(email);
		return ResponseEntity.ok("{\"message\":\"Аккаунт успешно удален\"}");
	}

	@PostMapping("/verify-token")
	public ResponseEntity<?> checkToken(@RequestHeader("Authorization") String token) {
		String email = userService.extractEmail(token);
		User user = userService.getUserByEmail(email);
		if (user == null) {
			return ResponseEntity.status(401).body("{\"error\":\"Неверный или просроченный токен\"}");
		}
		UserRole role = user.getRole();
		return ResponseEntity.ok("{\"role\":\"" + role + "\"}");
	}
}