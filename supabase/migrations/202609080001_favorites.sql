-- Bundled catalogue identifiers are shared across Home, Search, Category and Studio.
create table public.sticker_favorites (
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 sticker_id text not null check (sticker_id ~ '^(trending|frame|animal)-[0-2]$'),
 created_at timestamptz not null default now(),
 primary key (user_id, sticker_id)
);
alter table public.sticker_favorites enable row level security;
grant select, insert, update, delete on public.sticker_favorites to authenticated;
create policy "Own favorite stickers" on public.sticker_favorites for all to authenticated
using (user_id = auth.uid()) with check (user_id = auth.uid());
