alter table companion_profile add column proactive_enabled boolean not null default false;
create table proactive_message (
  id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade,
  content text not null, created_at timestamptz not null default now(), delivered_at timestamptz, read_at timestamptz
);
create index proactive_message_profile_unread_idx on proactive_message(profile_id, created_at desc) where read_at is null;
