package itmo.is.eventportal.entitie.enums;

public enum UserRole {
	ORGANIZER,
	PARTICIPANT,
	ADMIN;

	public static UserRole fromString(String role) {
		return switch (role) {
			case "organizer" -> ORGANIZER;
			case "participant" -> PARTICIPANT;
			case "admin" -> ADMIN;
			default -> null;
		};
	}

	public static String toString(UserRole role) {
		return switch (role) {
			case ORGANIZER -> "organizer";
			case PARTICIPANT -> "participant";
			case ADMIN -> "admin";
		};
	}
}
