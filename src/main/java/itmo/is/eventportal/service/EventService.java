package itmo.is.eventportal.service;

import itmo.is.eventportal.entitie.dto.EventDTO;
import itmo.is.eventportal.entitie.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {
	private final JdbcTemplate jdbcTemplate;
	private final UserService userService;

	public EventService(JdbcTemplate jdbcTemplate, UserService userService) {
		this.jdbcTemplate = jdbcTemplate;
		this.userService = userService;
	}

	public String addEvent(String token, EventDTO dto) {
		if (dto.getName() == null || dto.getDescription() == null || dto.getDate() == null || dto.getTime() == null || dto.getLocation() == null) {
			return "All event fields must be provided. " + dto;
		}

		String email = userService.extractEmail(token);
		String userRole = userService.getUserRole(token);
		if (!userRole.equals("admin") && !userRole.equals("organizer")) {
			return "Only organizers or admins can create events.";
		}

		String insertSql = """
				INSERT INTO "Event" ("name","description","date","time","location","organizer")
				SELECT ?, ?, CAST(? AS DATE), CAST(? AS TIME), ?, "ID" FROM "User" WHERE LOWER("email") = LOWER(?)
				""";
		jdbcTemplate.update(insertSql, dto.getName(), dto.getDescription(), dto.getDate(), dto.getTime(), dto.getLocation(), email);
		return "Event created successfully.";
	}

	public String updateEvent(String token, int eventId, EventDTO dto) {
		String role = userService.getUserRole(token);
		String email = userService.extractEmail(token);

		if (role.equals("admin")) {
			String sql = """
					    UPDATE "Event"
					    SET "name" = ?, "description" = ?, "date" = CAST(? AS DATE), "time" = CAST(? AS TIME), "location" = ?
					    WHERE "ID" = ?
					""";
			jdbcTemplate.update(sql, dto.getName(), dto.getDescription(), dto.getDate(), dto.getTime(), dto.getLocation(), eventId);
			return "Event updated successfully.";
		}

		String checkOrganizerSql = """
				    SELECT COUNT(*) FROM "Event" WHERE "ID" = ? AND "organizer" = (SELECT "ID" FROM "User" WHERE LOWER("email") = LOWER(?))
				""";
		Integer count = jdbcTemplate.queryForObject(checkOrganizerSql, Integer.class, eventId, email);
		if (count != null && count > 0) {
			String sql = """
					    UPDATE "Event"
					    SET "name" = ?, "description" = ?, "date" = CAST(? AS DATE), "time" = CAST(? AS TIME), "location" = ?
					    WHERE "ID" = ?
					""";
			jdbcTemplate.update(sql, dto.getName(), dto.getDescription(), dto.getDate(), dto.getTime(), dto.getLocation(), eventId);
			return "Event updated successfully.";
		}

		return "Only the event organizer or admins can edit events.";
	}

	public String deleteEvent(String token, int eventId) {
		System.out.println("Deleting event with id " + eventId);
		String role = userService.getUserRole(token);
		String email = userService.extractEmail(token);

		if (role.equals("admin")) {
			String sql = "DELETE FROM \"Event\" WHERE \"ID\" = ?";
			jdbcTemplate.update(sql, eventId);
			return "Event deleted.";
		}

		String checkOrganizerSql = """
				    SELECT COUNT(*) FROM "Event" WHERE "ID" = ? AND "organizer" = (SELECT "ID" FROM "User" WHERE LOWER("email") = LOWER(?))
				""";
		Integer count = jdbcTemplate.queryForObject(checkOrganizerSql, Integer.class, eventId, email);
		if (count != null && count > 0) {
			String sql = "DELETE FROM \"Event\" WHERE \"ID\" = ?";
			jdbcTemplate.update(sql, eventId);
			return "Event deleted.";
		}

		return "Only the event organizer or admins can delete events.";
	}

	public List<EventDTO> getAllEvents() {
		String sql = """
				    SELECT "ID", "name", "description", "date", "time", "location"
				    FROM "Event"
				""";
		return jdbcTemplate.query(sql, (rs, rowNum) -> new EventDTO(
				rs.getInt("ID"),
				rs.getString("name"),
				rs.getString("description"),
				rs.getString("date"),
				rs.getString("time"),
				rs.getString("location")
		));
	}

	public String registerForEvent(String token, int eventId) {
		String email = userService.extractEmail(token);
		if (email == null) {
			return "Invalid token.";
		}
		User user = userService.getUserByEmail(email);
		if (user == null) {
			return "User not found.";
		}
		String checkRegistrationSql = """
				    SELECT COUNT(*) FROM "Registration" WHERE "event" = ? AND "member" = ?
				""";
		Integer count = jdbcTemplate.queryForObject(checkRegistrationSql, Integer.class, eventId, user.getId());
		if (count != null && count > 0) {
			return "Already registered.";
		}
		String registerSql = """
				    INSERT INTO "Registration" ("event","member") VALUES (?,?)
				""";
		jdbcTemplate.update(registerSql, eventId, user.getId());
		return "Registration successful.";
	}
}