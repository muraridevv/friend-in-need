create table app_user (
  id uuid primary key,
  username varchar(80) not null unique,
  password_hash varchar(255) not null,
  created_at timestamptz not null default now()
);
