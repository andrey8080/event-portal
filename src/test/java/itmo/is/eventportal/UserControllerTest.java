package itmo.is.eventportal;

import itmo.is.eventportal.entitie.enums.UserRole;
import itmo.is.eventportal.entitie.model.User;
import itmo.is.eventportal.service.UserService;
import itmo.is.eventportal.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.security.enabled=false")
public class UserControllerTest {

	private final String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYSIsImlhdCI6MTc0MDI1ODE3OCwiZXhwIjoxNzQwMzQ0NTc4fQ.5Z6nxHkpIcayk77wvYn7SB2Dj3EmAdmyJtBP3Cw7ssL-JlrJnsXdq6cfg8RpbPjmhbwoe9oI-lg79dCMWj78OQ";
	@Autowired
	private TestRestTemplate restTemplate;
	@Mock
	private UserService userService;
	@Mock
	private JwtTokenUtil jwtTokenUtil;
	private User user;

	@BeforeEach
	public void setUp() {
		user = new User(1, "John Doe", "john.doe@example.com", "password", "1234567890", UserRole.PARTICIPANT);
	}

	@Test
	public void testSigninForm() {
		given(userService.getUserByEmail(anyString())).willReturn(user);
		given(jwtTokenUtil.generateJwtToken(anyString())).willReturn("mockedToken");

		String url = UriComponentsBuilder.fromPath("/user/signin")
				.queryParam("email", user.getEmail())
				.queryParam("password", user.getPassword())
				.toUriString();

		ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
		assertEquals(200, response.getStatusCodeValue());
	}

	@Test
	public void testSignupForm() {
		given(userService.getUserByEmail(anyString())).willReturn(null);
		doNothing().when(userService).createUser(org.mockito.ArgumentMatchers.any(User.class));
		given(jwtTokenUtil.generateJwtToken(anyString())).willReturn("mockedToken");

		User signupUser = new User(1, "John Doe", "john.doe@example.com", "password", "1234567890", UserRole.PARTICIPANT);
		ResponseEntity<String> response = restTemplate.postForEntity("/user/signup", signupUser, String.class);
		assertEquals(200, response.getStatusCodeValue());
		System.out.println(response.getBody());
	}

	@Test
	public void testChangeData() {
		given(userService.extractEmail(anyString())).willReturn(user.getEmail());
		given(userService.getUserByEmail(anyString())).willReturn(user);
		doNothing().when(userService).updateUser(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(User.class));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", token);
		User updatedUser = new User(1, "John Doe Updated", "ыыыыы", "фф", "9876543210", UserRole.PARTICIPANT);
		HttpEntity<User> request = new HttpEntity<>(updatedUser, headers);
		ResponseEntity<String> response = restTemplate.exchange("/user/update", HttpMethod.PUT, request, String.class);
		assertEquals(200, response.getStatusCodeValue());
		assertEquals("{\"message\":\"Данные успешно изменены\"}", response.getBody());
	}

//	@Test
//	public void testDeleteAccount() {
//		given(userService.extractEmail(anyString())).willReturn(user.getEmail());
//		doNothing().when(userService).deleteUserByEmail(anyString());
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("Authorization", token);
//		HttpEntity<Void> request = new HttpEntity<>(headers);
//		ResponseEntity<String> response = restTemplate.exchange("/user/delete", HttpMethod.DELETE, request, String.class);
//		assertEquals(200, response.getStatusCodeValue());
//		assertEquals("{\"message\":\"Аккаунт успешно удален\"}", response.getBody());
//	}

	@Test
	public void testCheckToken() {
		given(userService.extractEmail(anyString())).willReturn(user.getEmail());
		given(userService.getUserByEmail(anyString())).willReturn(user);

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", token);
		HttpEntity<Void> request = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.postForEntity("/user/verify-token", request, String.class);
		assertEquals(200, response.getStatusCodeValue());
		assertEquals("{\"role\":\"PARTICIPANT\"}", response.getBody());
	}
}
