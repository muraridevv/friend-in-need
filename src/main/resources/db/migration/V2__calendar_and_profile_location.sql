alter table companion_profile add column location varchar(120) not null default 'New York';
create table calendar_event (
  id uuid primary key,
  profile_id uuid not null references companion_profile(id) on delete cascade,
  title varchar(160) not null,
  starts_at timestamptz not null,
  ends_at timestamptz,
  created_at timestamptz not null default now()
);
create index calendar_event_profile_starts_idx on calendar_event(profile_id, starts_at);
