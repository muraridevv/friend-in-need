create table companion_timer (id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade, label varchar(500) not null, trigger_at timestamptz not null, fired boolean not null default false);
create index companion_timer_due_idx on companion_timer(fired, trigger_at);
