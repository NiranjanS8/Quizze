create table roles (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    name varchar(20) not null unique,
    description varchar(255)
);

create table users (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(150) not null unique,
    username varchar(50) not null unique,
    password varchar(255) not null,
    enabled boolean not null,
    new_quiz_notifications_enabled boolean default false not null,
    role_id bigint not null references roles (id)
);

create table categories (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    name varchar(100) not null unique,
    description varchar(255)
);

create table quizzes (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    title varchar(150) not null,
    description varchar(1000),
    category_id bigint references categories (id),
    difficulty varchar(20) not null,
    time_limit_in_minutes integer not null,
    published boolean not null,
    negative_marking_enabled boolean not null,
    one_attempt_only boolean not null
);

create table questions (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    content varchar(1000) not null,
    quiz_id bigint not null references quizzes (id) on delete cascade,
    points integer not null
);

create table options (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    content varchar(500) not null,
    correct boolean not null,
    question_id bigint not null references questions (id) on delete cascade
);

create table quiz_attempts (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    quiz_id bigint not null references quizzes (id),
    user_id bigint not null references users (id),
    status varchar(20) not null,
    started_at timestamp(6),
    submitted_at timestamp(6),
    question_order varchar(2000),
    score double precision not null,
    correct_answers integer not null,
    wrong_answers integer not null
);

create table attempt_answers (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    attempt_id bigint not null references quiz_attempts (id) on delete cascade,
    question_id bigint not null references questions (id),
    selected_option_id bigint references options (id),
    correct boolean
);

create table password_reset_otps (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    user_id bigint not null references users (id),
    email varchar(150) not null,
    otp_hash varchar(255) not null,
    expires_at timestamp(6) not null,
    failed_attempts integer not null,
    used_at timestamp(6)
);

create table quiz_analytics_projections (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    quiz_id bigint not null unique,
    quiz_title varchar(180) not null,
    category_name varchar(120),
    submitted_attempts bigint not null,
    average_score double precision not null,
    average_percentage double precision not null,
    highest_score double precision not null,
    lowest_score double precision not null,
    max_score double precision not null,
    average_correct_answers double precision not null,
    average_wrong_answers double precision not null,
    last_submitted_at timestamp(6)
);

create table user_notifications (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    user_id bigint not null references users (id),
    type varchar(40) not null,
    title varchar(180) not null,
    message varchar(500) not null,
    read boolean not null,
    related_quiz_id bigint,
    related_attempt_id bigint
);

create table processed_kafka_events (
    id bigserial primary key,
    event_id varchar(120) not null unique,
    event_type varchar(80) not null,
    consumer_name varchar(120) not null,
    processed_at timestamp(6) not null
);

create table admin_audit_logs (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    admin_user_id bigint not null,
    admin_username varchar(50) not null,
    action_type varchar(40) not null,
    target_type varchar(30) not null,
    target_id bigint not null,
    target_name varchar(255) not null,
    description varchar(1000) not null
);

create table event_stream_entries (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    event_type varchar(120) not null,
    aggregate_type varchar(80) not null,
    aggregate_id bigint not null,
    actor_user_id bigint,
    summary varchar(2000)
);

create index idx_users_role_id on users (role_id);
create index idx_quizzes_category_id on quizzes (category_id);
create index idx_questions_quiz_id on questions (quiz_id);
create index idx_options_question_id on options (question_id);
create index idx_quiz_attempts_user_quiz on quiz_attempts (user_id, quiz_id);
create index idx_quiz_attempts_quiz_status on quiz_attempts (quiz_id, status);
create index idx_attempt_answers_attempt_id on attempt_answers (attempt_id);
create index idx_password_reset_otps_email_created_at on password_reset_otps (email, created_at desc);
create index idx_user_notifications_user_created_at on user_notifications (user_id, created_at desc);
create index idx_user_notifications_user_read on user_notifications (user_id, read);
create index idx_admin_audit_logs_created_at on admin_audit_logs (created_at desc);
create index idx_event_stream_entries_aggregate on event_stream_entries (aggregate_type, aggregate_id);
