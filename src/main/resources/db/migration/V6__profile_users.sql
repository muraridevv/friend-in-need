alter table companion_profile add column user_id uuid references app_user(id);
create index companion_profile_user_idx on companion_profile(user_id);
