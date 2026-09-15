create extension if not exists vector;
alter table companion_memory add column embedding_vec vector(1536);
-- Existing serialized embeddings require a one-time application-specific backfill before this migration.
alter table companion_memory drop column embedding;
alter table companion_memory drop column embedding_model;
alter table companion_memory rename column embedding_vec to embedding;
create index memory_embedding_idx on companion_memory using hnsw (embedding vector_cosine_ops);
