
create table if not exists test_profiles (
    id bigserial primary key,
    user_id bigint references test_users(id) unique,
    site_url text not null
);
