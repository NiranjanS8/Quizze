BEGIN;

INSERT INTO categories (name, description, created_at, updated_at)
VALUES
    ('Java', 'Core Java programming fundamentals', NOW(), NOW()),
    ('Spring Boot', 'Spring Boot and REST API fundamentals', NOW(), NOW()),
    ('SQL', 'Relational database and query concepts', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO quizzes (
    title,
    description,
    category_id,
    difficulty,
    time_limit_in_minutes,
    published,
    negative_marking_enabled,
    one_attempt_only,
    created_at,
    updated_at
)
SELECT
    'Java Fundamentals Challenge',
    'Test object-oriented basics, collections, and Java syntax.',
    c.id,
    'EASY',
    15,
    TRUE,
    FALSE,
    FALSE,
    NOW(),
    NOW()
FROM categories c
WHERE c.name = 'Java'
  AND NOT EXISTS (
      SELECT 1 FROM quizzes q WHERE q.title = 'Java Fundamentals Challenge'
  );

INSERT INTO quizzes (
    title,
    description,
    category_id,
    difficulty,
    time_limit_in_minutes,
    published,
    negative_marking_enabled,
    one_attempt_only,
    created_at,
    updated_at
)
SELECT
    'Spring Boot API Mastery',
    'Covers Spring Boot annotations, REST APIs, dependency injection, and configuration.',
    c.id,
    'MEDIUM',
    20,
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    NOW()
FROM categories c
WHERE c.name = 'Spring Boot'
  AND NOT EXISTS (
      SELECT 1 FROM quizzes q WHERE q.title = 'Spring Boot API Mastery'
  );

INSERT INTO quizzes (
    title,
    description,
    category_id,
    difficulty,
    time_limit_in_minutes,
    published,
    negative_marking_enabled,
    one_attempt_only,
    created_at,
    updated_at
)
SELECT
    'SQL Query Builder',
    'Practice joins, grouping, filtering, and normalization basics.',
    c.id,
    'MEDIUM',
    18,
    FALSE,
    FALSE,
    FALSE,
    NOW(),
    NOW()
FROM categories c
WHERE c.name = 'SQL'
  AND NOT EXISTS (
      SELECT 1 FROM quizzes q WHERE q.title = 'SQL Query Builder'
  );

INSERT INTO questions (content, quiz_id, points, created_at, updated_at)
SELECT
    question_text,
    q.id,
    points,
    NOW(),
    NOW()
FROM (
    VALUES
        ('Java Fundamentals Challenge', 'Which keyword is used to inherit a class in Java?', 5),
        ('Java Fundamentals Challenge', 'Which collection stores unique values only?', 5),
        ('Java Fundamentals Challenge', 'What is the size of an int in Java?', 5),
        ('Java Fundamentals Challenge', 'Which method is the entry point of a Java application?', 5),
        ('Java Fundamentals Challenge', 'Which keyword is used to create a subclass object reference using a parent type?', 5),

        ('Spring Boot API Mastery', 'Which annotation marks the main Spring Boot application class?', 5),
        ('Spring Boot API Mastery', 'Which annotation is used for constructor or field dependency injection?', 5),
        ('Spring Boot API Mastery', 'Which annotation maps HTTP GET requests in Spring MVC?', 5),
        ('Spring Boot API Mastery', 'Which file is commonly used for application configuration in Spring Boot?', 5),
        ('Spring Boot API Mastery', 'Which Spring Boot starter is typically used for building REST APIs?', 5),

        ('SQL Query Builder', 'Which SQL clause is used to filter rows after grouping?', 5),
        ('SQL Query Builder', 'Which join returns only matching rows from both tables?', 5),
        ('SQL Query Builder', 'Which keyword removes duplicate rows from a SELECT result?', 5),
        ('SQL Query Builder', 'Which aggregate function counts rows?', 5),
        ('SQL Query Builder', 'Which normal form removes partial dependency?', 5)
) AS seed(quiz_title, question_text, points)
JOIN quizzes q ON q.title = seed.quiz_title
WHERE NOT EXISTS (
    SELECT 1
    FROM questions existing
    WHERE existing.quiz_id = q.id
      AND existing.content = seed.question_text
);

INSERT INTO options (content, correct, question_id, created_at, updated_at)
SELECT
    option_text,
    is_correct,
    q.id,
    NOW(),
    NOW()
FROM (
    VALUES
        ('Which keyword is used to inherit a class in Java?', 'implements', FALSE),
        ('Which keyword is used to inherit a class in Java?', 'extends', TRUE),
        ('Which keyword is used to inherit a class in Java?', 'inherits', FALSE),
        ('Which keyword is used to inherit a class in Java?', 'super', FALSE),

        ('Which collection stores unique values only?', 'List', FALSE),
        ('Which collection stores unique values only?', 'Map', FALSE),
        ('Which collection stores unique values only?', 'Set', TRUE),
        ('Which collection stores unique values only?', 'Queue', FALSE),

        ('What is the size of an int in Java?', '16 bits', FALSE),
        ('What is the size of an int in Java?', '32 bits', TRUE),
        ('What is the size of an int in Java?', '64 bits', FALSE),
        ('What is the size of an int in Java?', '128 bits', FALSE),

        ('Which method is the entry point of a Java application?', 'start()', FALSE),
        ('Which method is the entry point of a Java application?', 'init()', FALSE),
        ('Which method is the entry point of a Java application?', 'main()', TRUE),
        ('Which method is the entry point of a Java application?', 'run()', FALSE),

        ('Which keyword is used to create a subclass object reference using a parent type?', 'this', FALSE),
        ('Which keyword is used to create a subclass object reference using a parent type?', 'super', FALSE),
        ('Which keyword is used to create a subclass object reference using a parent type?', 'polymorphism', FALSE),
        ('Which keyword is used to create a subclass object reference using a parent type?', 'upcasting', TRUE),

        ('Which annotation marks the main Spring Boot application class?', '@EnableBoot', FALSE),
        ('Which annotation marks the main Spring Boot application class?', '@SpringBootApplication', TRUE),
        ('Which annotation marks the main Spring Boot application class?', '@BootApplication', FALSE),
        ('Which annotation marks the main Spring Boot application class?', '@SpringMain', FALSE),

        ('Which annotation is used for constructor or field dependency injection?', '@Bean', FALSE),
        ('Which annotation is used for constructor or field dependency injection?', '@Autowired', TRUE),
        ('Which annotation is used for constructor or field dependency injection?', '@ComponentScan', FALSE),
        ('Which annotation is used for constructor or field dependency injection?', '@Injectable', FALSE),

        ('Which annotation maps HTTP GET requests in Spring MVC?', '@RequestBody', FALSE),
        ('Which annotation maps HTTP GET requests in Spring MVC?', '@PostMapping', FALSE),
        ('Which annotation maps HTTP GET requests in Spring MVC?', '@GetMapping', TRUE),
        ('Which annotation maps HTTP GET requests in Spring MVC?', '@PathVariable', FALSE),

        ('Which file is commonly used for application configuration in Spring Boot?', 'pom.xml', FALSE),
        ('Which file is commonly used for application configuration in Spring Boot?', 'application.properties', TRUE),
        ('Which file is commonly used for application configuration in Spring Boot?', 'build.gradle', FALSE),
        ('Which file is commonly used for application configuration in Spring Boot?', 'settings.xml', FALSE),

        ('Which Spring Boot starter is typically used for building REST APIs?', 'spring-boot-starter-data-jpa', FALSE),
        ('Which Spring Boot starter is typically used for building REST APIs?', 'spring-boot-starter-security', FALSE),
        ('Which Spring Boot starter is typically used for building REST APIs?', 'spring-boot-starter-web', TRUE),
        ('Which Spring Boot starter is typically used for building REST APIs?', 'spring-boot-starter-test', FALSE),

        ('Which SQL clause is used to filter rows after grouping?', 'WHERE', FALSE),
        ('Which SQL clause is used to filter rows after grouping?', 'GROUP BY', FALSE),
        ('Which SQL clause is used to filter rows after grouping?', 'HAVING', TRUE),
        ('Which SQL clause is used to filter rows after grouping?', 'ORDER BY', FALSE),

        ('Which join returns only matching rows from both tables?', 'LEFT JOIN', FALSE),
        ('Which join returns only matching rows from both tables?', 'RIGHT JOIN', FALSE),
        ('Which join returns only matching rows from both tables?', 'INNER JOIN', TRUE),
        ('Which join returns only matching rows from both tables?', 'FULL JOIN', FALSE),

        ('Which keyword removes duplicate rows from a SELECT result?', 'UNIQUE', FALSE),
        ('Which keyword removes duplicate rows from a SELECT result?', 'DISTINCT', TRUE),
        ('Which keyword removes duplicate rows from a SELECT result?', 'FILTER', FALSE),
        ('Which keyword removes duplicate rows from a SELECT result?', 'GROUP', FALSE),

        ('Which aggregate function counts rows?', 'SUM()', FALSE),
        ('Which aggregate function counts rows?', 'COUNT()', TRUE),
        ('Which aggregate function counts rows?', 'TOTAL()', FALSE),
        ('Which aggregate function counts rows?', 'NUMBER()', FALSE),

        ('Which normal form removes partial dependency?', '1NF', FALSE),
        ('Which normal form removes partial dependency?', '2NF', TRUE),
        ('Which normal form removes partial dependency?', '3NF', FALSE),
        ('Which normal form removes partial dependency?', 'BCNF', FALSE)
) AS seed(question_text, option_text, is_correct)
JOIN questions q ON q.content = seed.question_text
WHERE NOT EXISTS (
    SELECT 1
    FROM options existing
    WHERE existing.question_id = q.id
      AND existing.content = seed.option_text
);

COMMIT;
