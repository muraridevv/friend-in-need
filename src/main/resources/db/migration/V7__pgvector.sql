create extension if not exists vector;
alter table companion_memory add column embedding_vec vector(1536);
-- Preserve legacy serialized embeddings until a verified application backfill has completed.
alter table companion_memory rename column embedding to embedding_legacy;
alter table companion_memory rename column embedding_model to embedding_model_legacy;
alter table companion_memory rename column embedding_vec to embedding;
create index memory_embedding_idx on companion_memory using hnsw (embedding vector_cosine_ops);
