# Architecture

## Design

The application follows a **ports-and-adapters** shape: HTTP controllers are adapters, `CompanionService` is the conversation use case, and OpenRouter is accessed through Spring AI's OpenAI-compatible `ChatClient`. Provider integrations belong behind explicit adapter interfaces so calendar, weather, and news access can be consent-gated and swapped without leaking into prompts.

`CompanionProfile` is the aggregate owner of personality, interests, timezone, and face-enrollment state. Conversation messages are immutable audit/memory records. A production deployment should add an authenticated subject key to every aggregate and enforce it at the repository boundary.

## Conversation and RAG

Each turn persists user and assistant messages, passes a bounded recent history and profile context to the model, and explicitly labels unavailable integration data. The next production increment is a `MemoryRetriever` port backed by PostgreSQL + pgvector: chunk consented journal/profile content, embed on write, retrieve top-k by cosine distance, and inject only citations/snippets within a token budget. Keep raw secrets and face data out of RAG.

## Proactive and integrations

A scheduled `ProactiveCheckIn` use case should query opted-in calendar/weather/news adapters, generate one low-pressure check-in, apply quiet hours/timezone limits, and store a delivery record. OAuth tokens must be envelope-encrypted and providers must be off by default. Use an outbox table for reliable notification delivery.

## Privacy and safety

The browser implementation derives camera templates and sends only those values; it intentionally is not identity-grade biometric recognition. Do not use it for authentication, access control, or safety decisions. Real face recognition requires informed consent, liveness detection, encrypted biometric templates, deletion/export controls, bias evaluation, rate limits, and a human-reviewed threat model. The companion system prompt discloses that it is not emergency care and routes imminent danger to local emergency services.

## Implemented vertical slice

The current project delivers local calendar events, live weather through Open-Meteo (without a user API key), and a profile memory store. Memory retrieval is deterministic lexical ranking, deliberately keeping the implementation observable and inexpensive. It is a working stepping stone rather than semantic RAG; once a memory corpus grows, substitute the documented pgvector retriever behind `MemoryService`.

## Three-model execution path

The conversation model remains the OpenRouter-configured Spring AI chat model. `EmbeddingService` uses a separately configured embedding model and stores its vector with each consented memory; retrieval uses cosine similarity and falls back to lexical ranking if the embedding provider is unavailable. `VoiceService` is a separate OpenAI-compatible STT/TTS adapter with its own `VOICE_*` base URL and key—OpenRouter is never used for audio. The browser records audio with `MediaRecorder`, uploads it for transcription, and plays the returned synthesis audio.

Face enrollment now creates a three 32×32 normalized luminance templates from a detected face (when the browser supports `FaceDetector`), and server-side verification uses Hamming similarity rather than exact-frame equality. This is still an intentional convenience signal, not authentication or identity-grade biometrics.

## API observability and documentation

Springdoc publishes OpenAPI at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html`. Voice logs identify the resolved provider base URL and the exact STT/TTS endpoint shape without logging API keys, audio, or text. For OpenAI specifically, a bare `https://api.openai.com` base URL is normalized to `https://api.openai.com/v1/` so the audio routes do not fail from a missing API version segment.

## Multi-user profile selection

Face login calls `POST /api/profiles/recognize`, evaluates the submitted templates against enrolled profiles, and selects the highest recognized confidence. The browser does not restore a prior profile after reload, which prevents a shared device from silently continuing the last person's conversation. Every chat request remains profile-scoped; production deployments should additionally bind that profile to an authenticated principal and enforce it at the service boundary.
