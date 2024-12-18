CREATE TABLE "User"
(
    "ID"       SERIAL PRIMARY KEY,
    "name"     VARCHAR(100)        NOT NULL,
    "email"    VARCHAR(255) UNIQUE NOT NULL,
    "password" VARCHAR(255)        NOT NULL,
    "role"     VARCHAR(50) CHECK ("role" IN ('organizer', 'participant', 'admin'))
);

CREATE TABLE "Event"
(
    "ID"          SERIAL PRIMARY KEY,
    "name"        VARCHAR(200) NOT NULL,
    "description" TEXT,
    "date"        DATE         NOT NULL,
    "time"        TIME         NOT NULL,
    "location"    VARCHAR(255),
    "organizer"   INT          NOT NULL,
    CONSTRAINT "FK_Event_Organizer"
        FOREIGN KEY ("organizer") REFERENCES "User" ("ID") ON DELETE CASCADE
);

CREATE TABLE "Quiz"
(
    "ID"           SERIAL PRIMARY KEY,
    "event"        INT NOT NULL,
    "description"  TEXT,
    "time_to_pass" INT CHECK ("time_to_pass" > 0),
    CONSTRAINT "FK_Quiz_Event"
        FOREIGN KEY ("event") REFERENCES "Event" ("ID") ON DELETE CASCADE
);

CREATE TABLE "UserQuizResult"
(
    "ID"       SERIAL PRIMARY KEY,
    "user"     INT NOT NULL,
    "quiz"     INT NOT NULL,
    "date_end" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "result"   FLOAT CHECK ("result" >= 0 AND "result" <= 100),
    CONSTRAINT "FK_UserQuizResult_User"
        FOREIGN KEY ("user") REFERENCES "User" ("ID") ON DELETE CASCADE,
    CONSTRAINT "FK_UserQuizResult_Quiz"
        FOREIGN KEY ("quiz") REFERENCES "Quiz" ("ID") ON DELETE CASCADE
);


--------------------------------------------------------------------------------
DO $$
    BEGIN
        FOR i IN 1..100 LOOP
                INSERT INTO "User" ("name", "email", "password", "role")
                VALUES (
                           CONCAT('User_', i),
                           CONCAT('user', i, '@example.com'),
                           'password',
                           CASE
                               WHEN i % 3 = 0 THEN 'admin'
                               WHEN i % 2 = 0 THEN 'organizer'
                               ELSE 'participant'
                               END
                       );
            END LOOP;
    END $$;

DO $$
    DECLARE
        event_id INT;
    BEGIN
        -- Сначала создадим события, чтобы квизы имели ссылки
        FOR i IN 1..1000 LOOP
                INSERT INTO "Event" ("name", "description", "date", "time", "location", "organizer")
                VALUES (
                           CONCAT('Event_', i),
                           'Description of event ' || i,
                           CURRENT_DATE + (i % 30),
                           '10:00:00',
                           CONCAT('Location ', i),
                           (i % 100) + 1
                       )
                RETURNING "ID" INTO event_id;

                -- Затем добавляем квизы
                INSERT INTO "Quiz" ("event", "description", "time_to_pass")
                VALUES (
                           event_id,
                           CONCAT('Quiz for event ', event_id),
                           60 + (i % 120)
                       );
            END LOOP;
    END $$;


DO $$
    DECLARE
        i INT;
        user_id INT;
        quiz_id INT;
    BEGIN
        -- Предположим, что у нас уже есть 100 пользователей и 1000 квизов
        FOR i IN 1..10000000 LOOP
                user_id := FLOOR(RANDOM() * 100 + 1)::INT; -- Случайный пользователь от 1 до 100
                quiz_id := FLOOR(RANDOM() * 1000 + 1)::INT; -- Случайный квиз от 1 до 1000

                INSERT INTO "UserQuizResult" ("user", "quiz", "result")
                VALUES (user_id, quiz_id, FLOOR(RANDOM() * 100)::FLOAT);
            END LOOP;
    END $$;


EXPLAIN ANALYZE
SELECT *
FROM "UserQuizResult"
WHERE "user" = 10 AND "quiz" = 500;


CREATE INDEX idx_user_quiz_result ON "UserQuizResult" ("user", "quiz");


EXPLAIN ANALYZE
SELECT *
FROM "UserQuizResult"
WHERE "user" = 10 AND "quiz" = 500;
