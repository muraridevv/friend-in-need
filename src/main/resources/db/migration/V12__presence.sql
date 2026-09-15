create table presence_event (id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade, status varchar(16) not null, face_count integer, created_at timestamptz not null default now());
create index presence_event_profile_created_idx on presence_event(profile_id, created_at desc);
