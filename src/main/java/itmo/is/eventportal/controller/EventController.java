package itmo.is.eventportal.controller;

import itmo.is.eventportal.entitie.dto.EventDTO;
import itmo.is.eventportal.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event")
public class EventController {
	private final EventService eventService;

	@Autowired
	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@PostMapping("/add")
	public ResponseEntity<?> addEvent(@RequestHeader("Authorization") String token, @RequestBody EventDTO dto) {
		String result = eventService.addEvent(token, dto);
		if (result.startsWith("Event")) {
			return ResponseEntity.ok("{\"message\":\"" + result + "\"}");
		}
		return ResponseEntity.status(403).body("{\"error\":\"" + result + "\"}");
	}

	@PutMapping("/update")
	public ResponseEntity<?> editEvent(@RequestHeader("Authorization") String token,
	                                   @RequestBody EventDTO dto) {
		String result = eventService.updateEvent(token, dto.getId(), dto);
		if (result.startsWith("Event")) {
			return ResponseEntity.ok("{\"message\":\"" + result + "\"}");
		}
		return ResponseEntity.status(403).body("{\"error\":\"" + result + "\"}");
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> removeEvent(@RequestHeader("Authorization") String token, @RequestParam("id") int eventId) {
		System.out.println("пытаюсь удалить");
		String result = eventService.deleteEvent(token, eventId);
		if (result.equals("Event deleted.")) {
			return ResponseEntity.ok("{\"message\":\"" + result + "\"}");
		}
		return ResponseEntity.status(403).body("{\"error\":\"" + result + "\"}");
	}

	@GetMapping("/all")
	public ResponseEntity<?> getAllEvents() {
		return ResponseEntity.ok(eventService.getAllEvents());
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerForEvent(@RequestHeader("Authorization") String token,
	                                          @RequestParam("id") int eventId) {
		String result = eventService.registerForEvent(token, eventId);
		if (result.equals("Registration successful.")) {
			return ResponseEntity.ok("{\"message\":\"" + result + "\"}");
		}
		return ResponseEntity.status(403).body("{\"error\":\"" + result + "\"}");
	}
}