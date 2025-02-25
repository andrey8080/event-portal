package itmo.is.eventportal.entitie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class EventDTO {
	private int id;
	private String name;
	private String description;
	private String date;
	private String time;
	private String location;

	@Override
	public String toString() {
		return "EventDTO{" +
				"id=" + id +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", date='" + date + '\'' +
				", time='" + time + '\'' +
				", location='" + location + '\'' +
				'}';
	}
}
