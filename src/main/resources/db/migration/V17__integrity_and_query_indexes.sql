-- Corrective constraints and indexes for high-volume and uniqueness-sensitive tables.
alter table oauth_token add constraint oauth_token_profile_provider_key unique (profile_id, provider);
alter table conversation_message add constraint conversation_message_emotion_confidence_check check (emotion_confidence is null or (emotion_confidence >= 0 and emotion_confidence <= 1));
alter table presence_event add constraint presence_event_face_count_check check (face_count is null or face_count >= 0);
alter table gesture_event add constraint gesture_event_type_check check (gesture_type in ('WAVE','NOD','SHAKE','POINT','UNKNOWN'));
alter table routine add constraint routine_profile_trigger_phrase_key unique (profile_id, trigger_phrase);
alter table companion_timer add column if not exists created_at timestamptz not null default now();
alter table companion_timer add column if not exists cancelled boolean not null default false;
create index if not exists calendar_event_reminded_starts_idx on calendar_event(reminded, starts_at);
create index if not exists gesture_event_profile_created_idx on gesture_event(profile_id, created_at desc);
create index if not exists scene_observation_profile_created_idx on scene_observation(profile_id, created_at desc);
create index if not exists companion_timer_profile_trigger_idx on companion_timer(profile_id, trigger_at);
