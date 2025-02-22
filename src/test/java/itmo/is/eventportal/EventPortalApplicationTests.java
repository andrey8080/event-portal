package itmo.is.eventportal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EventPortalApplicationTests {

	@Test
	void contextLoads() {
	}

	@Nested
	class UserControllerTests extends UserControllerTest {
	}
}