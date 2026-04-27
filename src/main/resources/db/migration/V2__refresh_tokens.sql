create table refresh_tokens (
    id bigserial primary key,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    user_id bigint not null references users (id),
    token_hash varchar(64) not null unique,
    expires_at timestamp(6) not null,
    revoked_at timestamp(6)
);

create index idx_refresh_tokens_user_active on refresh_tokens (user_id, revoked_at);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);
