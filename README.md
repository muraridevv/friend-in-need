# friend in need

A Java 21 / Spring AI companion application with a voice-enabled web experience, profile personality, conversation memory, local camera-fingerprint enrollment, and a clean path to consented weather, calendar, news, proactive check-ins, and pgvector RAG.

## Run locally

1. Copy `.env.example` to `.env` and set `OPENROUTER_API_KEY`.
2. Start PostgreSQL 18: `docker compose up -d`.
3. Export the variables in `.env` and run `mvn spring-boot:run`.
4. Open `http://localhost:8080`.

The OpenRouter endpoint is configured through Spring AI's OpenAI-compatible client. The default model can be changed with `OPENROUTER_MODEL`.

## Product boundaries

Voice uses browser speech APIs. Camera enrollment stores a compact fingerprint, not an image; it is a demo convenience and **not authentication**. External integrations are intentionally opt-in adapters rather than silent data collection. See [architecture notes](docs/architecture.md).
