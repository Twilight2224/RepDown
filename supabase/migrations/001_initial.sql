-- Apply with `supabase db push`. No service-role key is exposed to either client.
create table if not exists public.repdawn_records (
  id uuid primary key, owner_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('folder','card','review','tombstone','sentence','idiom','mixture')),
  payload jsonb not null, revision bigint not null default 1, deleted_at timestamptz, updated_at timestamptz not null default now()
);
create index if not exists repdawn_records_owner_updated on public.repdawn_records(owner_id, updated_at);
alter table public.repdawn_records enable row level security;
create policy "owners read their records" on public.repdawn_records for select using (auth.uid() = owner_id);
create policy "owners insert their records" on public.repdawn_records for insert with check (auth.uid() = owner_id);
create policy "owners update their records" on public.repdawn_records for update using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "owners delete their records" on public.repdawn_records for delete using (auth.uid() = owner_id);
