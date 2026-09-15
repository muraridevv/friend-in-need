# Architecture

## Design

The application follows a **ports-and-adapters** shape: HTTP controllers are adapters, `CompanionService` is the conversation use case, and OpenRouter is accessed through Spring AI's OpenAI-compatible `ChatClient`. Provider integrations belong behind explicit adapter interfaces so calendar, weather, and news access can be consent-gated and swapped without leaking into prompts.

`CompanionProfile` is the aggregate owner of personality, interests, timezone, and face-enrollment state. Conversation messages are immutable audit/memory records. A production deployment should add an authenticated subject key to every aggregate and enforce it at the repository boundary.

## Conversation and RAG

Each turn persists user and assistant messages, passes a bounded recent history and profile context to the model, and explicitly labels unavailable integration data. The next production increment is a `MemoryRetriever` port backed by PostgreSQL + pgvector: chunk consented journal/profile content, embed on write, retrieve top-k by cosine distance, and inject only citations/snippets within a token budget. Keep raw secrets and face data out of RAG.

## Proactive and integrations

A scheduled `ProactiveCheckIn` use case should query opted-in calendar/weather/news adapters, generate one low-pressure check-in, apply quiet hours/timezone limits, and store a delivery record. OAuth tokens must be envelope-encrypted and providers must be off by default. Use an outbox table for reliable notification delivery.

## Privacy and safety

The browser implementation derives a tiny camera fingerprint and sends only that value; it intentionally is not identity-grade biometric recognition. Do not use it for authentication, access control, or safety decisions. Real face recognition requires informed consent, liveness detection, encrypted biometric templates, deletion/export controls, bias evaluation, rate limits, and a human-reviewed threat model. The companion system prompt discloses that it is not emergency care and routes imminent danger to local emergency services.

## Implemented vertical slice

The current project delivers local calendar events, live weather through Open-Meteo (without a user API key), and a profile memory store. Memory retrieval is deterministic lexical ranking, deliberately keeping the implementation observable and inexpensive. It is a working stepping stone rather than semantic RAG; once a memory corpus grows, substitute the documented pgvector retriever behind `MemoryService`.
