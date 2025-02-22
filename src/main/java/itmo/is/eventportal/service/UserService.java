package itmo.is.eventportal.service;

import itmo.is.eventportal.entitie.enums.UserRole;
import itmo.is.eventportal.entitie.model.User;
import itmo.is.eventportal.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final JwtTokenUtil jwtTokenUtil;
	private final JdbcTemplate jdbcTemplate;

	public UserService(JwtTokenUtil jwtTokenUtil, JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.jwtTokenUtil = jwtTokenUtil;
	}

	public void createUser(User user) {
		String normalizedEmail = user.getEmail().trim().toLowerCase();

		String checkUserSql = "SELECT COUNT(*) FROM \"User\" WHERE LOWER(\"email\") = LOWER(?)";
		Integer count = jdbcTemplate.queryForObject(checkUserSql, Integer.class, normalizedEmail);

		if (count != null && count > 0) {
			logger.warn("User already exists with email: {}", normalizedEmail);
			throw new IllegalStateException("Пользователь с таким email уже существует");
		} else {
			String insertSql = "INSERT INTO \"User\" (\"name\", \"email\", \"phoneNumber\", \"password\", \"role\") VALUES (?, ?, ?, ?, ?)";
			int rows = jdbcTemplate.update(insertSql,
					user.getName(),
					normalizedEmail,
					user.getPhoneNumber(),
					user.getPassword(),
					user.getStringRole());
			logger.info("Inserted {} rows", rows);
		}
	}

	public void updateUser(String token, User user) {
		String emailFromToken = extractEmail(token);

		String checkUserSql = "SELECT COUNT(*) FROM \"User\" WHERE LOWER(\"email\") = LOWER(?)";
		Integer count = jdbcTemplate.queryForObject(checkUserSql, Integer.class, emailFromToken);

		if (count == null || count == 0) {
			logger.warn("User not found with email: {}", emailFromToken);
			throw new IllegalStateException("Пользователь с таким email не найден");
		} else {
			String updateSql = "UPDATE \"User\" SET \"name\" = ?, \"email\" = ?, \"phoneNumber\" = ?, \"password\" = ?, \"role\" = ? WHERE LOWER(\"email\") = LOWER(?)";
			int rows = jdbcTemplate.update(updateSql,
					user.getName(),
					user.getEmail(),
					user.getPhoneNumber(),
					user.getPassword(),
					user.getStringRole(),
					emailFromToken
			);
			logger.info("Updated {} rows", rows);
		}
	}

	public User getUserByEmail(String email) {
		String sql = "SELECT * FROM \"User\" WHERE LOWER(\"email\") = LOWER(?)";
		try {
			return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new User(
					rs.getInt("ID"),
					rs.getString("name"),
					rs.getString("email"),
					rs.getString("phoneNumber"),
					rs.getString("password"),
					UserRole.fromString(rs.getString("role"))
			), email.trim().toLowerCase());
		} catch (EmptyResultDataAccessException e) {
			logger.error("User not found with email: {}", email);
			return null;
		}
	}

	public void deleteUserByEmail(String email) {
		String sql = "DELETE FROM \"User\" WHERE LOWER(\"email\") = LOWER(?)";
		jdbcTemplate.update(sql, email.trim().toLowerCase());
	}

	public boolean checkValidToken(String token) {
		if (token == null) {
			return false;
		}

		boolean validToken;
		try {
			validToken = jwtTokenUtil.validateJwtToken(token);
		} catch (Exception e) {
			logger.error("Token validation failed: {}", e.getMessage());
			return false;
		}
		return validToken;
	}

	public String extractEmail(String token) {
		if (!checkValidToken(token)) {
			return null;
		}
		String email;
		try {
			email = jwtTokenUtil.getEmailFromJwtToken(token);
		} catch (Exception e) {
			logger.error("Failed to extract email from token: {}", e.getMessage());
			return null;
		}
		return email;
	}
}
