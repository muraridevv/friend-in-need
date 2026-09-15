# friend in need

A Java 21 / Spring AI companion application with a voice-enabled web experience, profile personality, conversation memory, local camera-template enrollment, and a clean path to consented weather, calendar, news, proactive check-ins, and pgvector RAG.

## Run locally

1. Copy `.env.example` to `.env` and set `OPENROUTER_API_KEY`.
2. Start PostgreSQL 18: `docker compose up -d`.
3. Export the variables in `.env` and run `mvn spring-boot:run`.
4. Open `http://localhost:8080`.

The OpenRouter endpoint is configured through Spring AI's OpenAI-compatible client. The default model can be changed with `OPENROUTER_MODEL`.

## Product boundaries

Voice uses browser speech APIs. Camera enrollment stores compact face templates, not an image; it is a demo convenience and **not authentication**. External integrations are intentionally opt-in adapters rather than silent data collection. See [architecture notes](docs/architecture.md).

## Model configuration

Three independently configured models are used: `OPENROUTER_MODEL` for conversation, `EMBEDDING_MODEL` for RAG memory, and `VOICE_STT_MODEL` / `VOICE_TTS_MODEL` for speech. Voice must use a separate OpenAI-compatible provider via `VOICE_API_BASE_URL`; it is never sent to OpenRouter. Copy the values from `.env.example` before enabling voice.

## API documentation

After startup, interactive Swagger UI is available at [`/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html). The OpenAPI document is served from [`/v3/api-docs`](http://localhost:8080/v3/api-docs).

## Multiple people on one device

Each companion profile owns its personality, memories, conversations, calendar events, and face templates. A fresh page load starts at the face-login screen rather than restoring the previous browser user. Use **Switch user** to recognize a different enrolled person; only the recognized profile ID is sent with subsequent chat and integration requests.
