package itmo.is.eventportal.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.logging.Logger;

@Configuration
public class DatabaseInitializer {

	private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void initialize() {
		try {
			createTables();
			createConstraintsAndTriggers();
			createIndexes();
			createFunctions();
		} catch (Exception e) {
			logger.severe("Error during database initialization: " + e.getMessage());
			throw e;
		}
	}

	private void createTables() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "User" (
				    "ID" SERIAL PRIMARY KEY,
				    "name" VARCHAR(100) NOT NULL,
				    "email" VARCHAR(255) UNIQUE NOT NULL,
				    "phoneNumber" VARCHAR(50) UNIQUE NOT NULL,
				    "password" VARCHAR(255) NOT NULL,
				    "role" VARCHAR(50) CHECK ("role" IN ('organizer', 'participant', 'admin'))
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "Event" (
				    "ID" SERIAL PRIMARY KEY,
				    "name" VARCHAR(200) NOT NULL,
				    "description" TEXT,
				    "date" DATE NOT NULL,
				    "time" TIME NOT NULL,
				    "location" VARCHAR(255),
				    "organizer" INT NOT NULL,
				    CONSTRAINT "FK_Event_Organizer"
				        FOREIGN KEY ("organizer") REFERENCES "User" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "Registration" (
				    "ID" SERIAL PRIMARY KEY,
				    "event" INT NOT NULL,
				    "member" INT NOT NULL,
				    "date" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
				    CONSTRAINT "FK_Registration_Event"
				        FOREIGN KEY ("event") REFERENCES "Event" ("ID") ON DELETE CASCADE,
				    CONSTRAINT "FK_Registration_User"
				        FOREIGN KEY ("member") REFERENCES "User" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "Quiz" (
				    "ID" SERIAL PRIMARY KEY,
				    "event" INT NOT NULL,
				    "description" TEXT,
				    "time_to_pass" INT CHECK ("time_to_pass" > 0),
				    CONSTRAINT "FK_Quiz_Event"
				        FOREIGN KEY ("event") REFERENCES "Event" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "QuizQuestion" (
				    "ID" SERIAL PRIMARY KEY,
				    "quiz" INT NOT NULL,
				    "text" TEXT NOT NULL,
				    "type" VARCHAR(50) CHECK ("type" IN ('text', 'multiple_choice', 'single_choice')),
				    CONSTRAINT "FK_QuizQuestion_Quiz"
				        FOREIGN KEY ("quiz") REFERENCES "Quiz" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "QuizAnswer" (
				    "ID" SERIAL PRIMARY KEY,
				    "quiz_question" INT NOT NULL,
				    "text" TEXT NOT NULL,
				    CONSTRAINT "FK_QuizAnswer_QuizQuestion"
				        FOREIGN KEY ("quiz_question") REFERENCES "QuizQuestion" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "UserQuizResult" (
				    "ID" SERIAL PRIMARY KEY,
				    "user" INT NOT NULL,
				    "quiz" INT NOT NULL,
				    "date_end" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
				    "result" FLOAT CHECK ("result" >= 0 AND "result" <= 100),
				    CONSTRAINT "FK_UserQuizResult_User"
				        FOREIGN KEY ("user") REFERENCES "User" ("ID") ON DELETE CASCADE,
				    CONSTRAINT "FK_UserQuizResult_Quiz"
				        FOREIGN KEY ("quiz") REFERENCES "Quiz" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "Feedback" (
				    "ID" SERIAL PRIMARY KEY,
				    "user" INT NOT NULL,
				    "event" INT NOT NULL,
				    "rating" INT CHECK ("rating" BETWEEN 1 AND 5),
				    "comment" TEXT,
				    CONSTRAINT "FK_Feedback_User"
				        FOREIGN KEY ("user") REFERENCES "User" ("ID") ON DELETE CASCADE,
				    CONSTRAINT "FK_Feedback_Event"
				        FOREIGN KEY ("event") REFERENCES "Event" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "OrganizerStats" (
				    "ID" SERIAL PRIMARY KEY,
				    "event" INT NOT NULL UNIQUE,
				    "quantity_of_participants" INT CHECK ("quantity_of_participants" >= 0),
				    "medium_rating" FLOAT CHECK ("medium_rating" >= 0 AND "medium_rating" <= 5),
				    CONSTRAINT "FK_OrganizerStats_Event"
				        FOREIGN KEY ("event") REFERENCES "Event" ("ID") ON DELETE CASCADE
				);
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS "BlackListUsers" (
				    "ID" SERIAL PRIMARY KEY,
				    "user" INT NOT NULL UNIQUE,
				    "reason" TEXT NOT NULL,
				    CONSTRAINT "FK_BlackListUsers_User"
				        FOREIGN KEY ("user") REFERENCES "User" ("ID") ON DELETE CASCADE
				);
				""");
	}

	private void createConstraintsAndTriggers() {
		// Удаляем триггер, если он существует, чтобы избежать ошибки дублирования
		jdbcTemplate.execute("""
				DROP TRIGGER IF EXISTS increment_participants ON "Registration";
				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION update_participants_count()
				RETURNS TRIGGER AS $$
				BEGIN
				    UPDATE "OrganizerStats"
				    SET "quantity_of_participants" = "quantity_of_participants" + 1
				    WHERE "event" = NEW."event";
				    RETURN NEW;
				END;
				$$ LANGUAGE plpgsql;
				""");

		jdbcTemplate.execute("""
				CREATE TRIGGER increment_participants
				AFTER INSERT ON "Registration"
				FOR EACH ROW
				EXECUTE FUNCTION update_participants_count();
				""");

		// Удаляем триггер, если он существует
		jdbcTemplate.execute("""
				DROP TRIGGER IF EXISTS recalculate_rating ON "Feedback";
				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION update_medium_rating()
				RETURNS TRIGGER AS $$
				BEGIN
				    UPDATE "OrganizerStats"
				    SET "medium_rating" = (
				        SELECT AVG("rating")::FLOAT
				        FROM "Feedback"
				        WHERE "event" = NEW."event"
				    )
				    WHERE "event" = NEW."event";
				    RETURN NEW;
				END;
				$$ LANGUAGE plpgsql;
				""");

		jdbcTemplate.execute("""
				CREATE TRIGGER recalculate_rating
				AFTER INSERT OR UPDATE ON "Feedback"
				FOR EACH ROW
				EXECUTE FUNCTION update_medium_rating();
				""");
	}

	private void createIndexes() {
		jdbcTemplate.execute("""
				CREATE INDEX IF NOT EXISTS idx_user_quiz_result ON "UserQuizResult" ("user", "quiz");
				""");
	}

	private void createFunctions() {
//		jdbcTemplate.execute("""
//				CREATE OR REPLACE FUNCTION register_user(user_name TEXT, user_email TEXT, user_password TEXT)
//				RETURNS TEXT AS $$
//				DECLARE
//				    user_exists BOOLEAN;
//				BEGIN
//				    SELECT EXISTS (SELECT 1 FROM "User" WHERE "email" = user_email) INTO user_exists;
//				    IF user_exists THEN
//				        RETURN 'Пользователь с таким email уже существует. Пожалуйста, войдите в систему';
//				    ELSE
//				        INSERT INTO "User" ("name", "email", "password", "role")
//				        VALUES (user_name, user_email, user_password, 'participant');
//				        RETURN 'Регистрация успешна! Проверьте вашу почту для подтверждения.';
//				    END IF;
//				END;
//				$$ LANGUAGE plpgsql;
//				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION create_event(
				    event_name TEXT,
				    event_description TEXT,
				    event_date DATE,
				    event_time TIME,
				    event_location TEXT,
				    organizer_id INT,
				    quiz_description TEXT DEFAULT NULL,
				    quiz_time_to_pass INT DEFAULT NULL
				) RETURNS TEXT AS $$
				DECLARE
				    event_id INT;
				BEGIN
				    IF NOT EXISTS (SELECT 1 FROM "User" WHERE "ID" = organizer_id AND "role" = 'organizer') THEN
				        RETURN 'Ошибка: Только авторизованные организаторы могут создавать мероприятия.';
				    END IF;
				    INSERT INTO "Event" ("name", "description", "date", "time", "location", "organizer")
				    VALUES (event_name, event_description, event_date, event_time, event_location, organizer_id)
				    RETURNING "ID" INTO event_id;
				    RETURN 'Мероприятие успешно создано.';
				END;
				$$ LANGUAGE plpgsql;
				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION register_for_event(event_id INT, user_id INT)
				RETURNS TEXT AS $$
				DECLARE
				    already_registered BOOLEAN;
				BEGIN
				    SELECT EXISTS (SELECT 1 FROM "Registration" WHERE "event" = event_id AND "member" = user_id) INTO already_registered;
				    IF already_registered THEN
				        RETURN 'Вы уже зарегистрированы на это мероприятие.';
				    END IF;
				    INSERT INTO "Registration" ("event", "member") VALUES (event_id, user_id);
				    RETURN 'Вы успешно зарегистрировались на мероприятие.';
				END;
				$$ LANGUAGE plpgsql;
				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION save_quiz_result(user_id INT, quiz_id INT, result FLOAT)
				RETURNS TEXT AS $$
				BEGIN
				    IF NOT EXISTS (
				        SELECT 1 FROM "Registration" r
				        JOIN "Event" e ON r."event" = e."ID"
				        WHERE r."member" = user_id AND e."ID" = (SELECT "event" FROM "Quiz" WHERE "ID" = quiz_id)
				    ) THEN
				        RETURN 'Ошибка: Вы не зарегистрированы на это мероприятие.';
				    END IF;
				    INSERT INTO "UserQuizResult" ("user", "quiz", "result")
				    VALUES (user_id, quiz_id, result)
				    ON CONFLICT ("user", "quiz") DO UPDATE
				    SET "result" = EXCLUDED."result";
				    RETURN 'Результат квиза успешно сохранён.';
				END;
				$$ LANGUAGE plpgsql;
				""");

		jdbcTemplate.execute("""
				CREATE OR REPLACE FUNCTION leave_feedback(user_id INT, event_id INT, rating INT, comment TEXT)
				RETURNS TEXT AS $$
				BEGIN
				    IF NOT EXISTS (SELECT 1 FROM "Registration" WHERE "event" = event_id AND "member" = user_id) THEN
				        RETURN 'Ошибка: Вы не участвовали в этом мероприятии.';
				    END IF;
				    INSERT INTO "Feedback" ("user", "event", "rating", "comment")
				    VALUES (user_id, event_id, rating, comment);
				    RETURN 'Отзыв успешно добавлен.';
				END;
				$$ LANGUAGE plpgsql;
				""");
	}
}
