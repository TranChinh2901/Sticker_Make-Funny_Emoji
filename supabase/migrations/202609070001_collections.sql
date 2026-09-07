-- Apply through your Supabase migration workflow before using collection creation.
create table public.sticker_collections (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    name text not null check (char_length(btrim(name)) between 1 and 80),
    background_color text not null default '#F8FBFF'
        check (background_color ~ '^#[0-9A-Fa-f]{6}$'),
    created_at timestamptz not null default now()
);
create index sticker_collections_user_id_idx on public.sticker_collections(user_id);
alter table public.sticker_collections enable row level security;
grant select, insert, update, delete on public.sticker_collections to authenticated;
create policy "Read own collections" on public.sticker_collections for select to authenticated using (user_id = auth.uid());
create policy "Create own collections" on public.sticker_collections for insert to authenticated with check (user_id = auth.uid());
create policy "Update own collections" on public.sticker_collections for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "Delete own collections" on public.sticker_collections for delete to authenticated using (user_id = auth.uid());
