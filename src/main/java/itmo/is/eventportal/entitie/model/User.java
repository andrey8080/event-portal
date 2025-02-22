package itmo.is.eventportal.entitie.model;

import itmo.is.eventportal.entitie.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
	private int id;
	private String name;
	private String email;
	private String phoneNumber;
	private String password;
	private UserRole role;

	public User(String name, String email, String password) {
		this.name = name;
		this.email = email;
		this.password = password;
	}

	public String getStringRole() {
		return UserRole.toString(role);
	}
}