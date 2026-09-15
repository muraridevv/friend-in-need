create table companion_profile (
  id uuid primary key, display_name varchar(80) not null, personality text not null,
  interests text not null default '', timezone varchar(80) not null default 'UTC',
  face_fingerprint varchar(128), face_enrolled_at timestamptz, created_at timestamptz not null default now()
);
create table conversation_message (
  id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade,
  role varchar(16) not null check (role in ('USER','ASSISTANT','SYSTEM')), content text not null,
  created_at timestamptz not null default now()
);
create index conversation_message_profile_created_idx on conversation_message(profile_id, created_at desc);
create table companion_memory (
  id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade,
  content text not null, importance integer not null default 1, created_at timestamptz not null default now()
);
