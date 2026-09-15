create table routine (id uuid primary key, profile_id uuid not null references companion_profile(id) on delete cascade, trigger_phrase varchar(255) not null, steps text not null);
create index routine_profile_idx on routine(profile_id);
