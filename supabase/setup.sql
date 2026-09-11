-- Run once on a fresh Supabase project in SQL Editor.
-- All statements commit together; no partial schema on failure.
begin;

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


alter table public.sticker_collections add column thumbnail_path text;
alter table public.sticker_collections add constraint collection_thumbnail_owner
check (thumbnail_path is null or split_part(thumbnail_path, '/', 1) = user_id::text);

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('collection-thumbnails', 'collection-thumbnails', false, 5242880, array['image/png']);

create policy "Read own collection thumbnails" on storage.objects
for select to authenticated using (
 bucket_id = 'collection-thumbnails' and (storage.foldername(name))[1] = auth.uid()::text
);
create policy "Upload own collection thumbnails" on storage.objects
for insert to authenticated with check (
 bucket_id = 'collection-thumbnails' and (storage.foldername(name))[1] = auth.uid()::text
);
create policy "Delete own collection thumbnails" on storage.objects
for delete to authenticated using (
 bucket_id = 'collection-thumbnails' and (storage.foldername(name))[1] = auth.uid()::text
);


-- Private guest-owned studio. Apply after 001 and 002.
create table public.stickers (
 id uuid primary key,
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 name text not null check (char_length(btrim(name)) between 1 and 80),
 image_path text not null,
 created_at timestamptz not null default now(),
 unique (id, user_id),
 check (split_part(image_path, '/', 1) = user_id::text)
);
alter table public.sticker_collections add constraint collection_id_owner unique (id, user_id);
create table public.collection_stickers (
 collection_id uuid not null,
 sticker_id uuid not null,
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 created_at timestamptz not null default now(),
 primary key (collection_id, sticker_id),
 foreign key (collection_id, user_id) references public.sticker_collections(id, user_id) on delete cascade,
 foreign key (sticker_id, user_id) references public.stickers(id, user_id) on delete cascade
);
create table public.user_settings (
 user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
 language text not null default 'en' check (language in ('fr','en','vi','hi','es','zh','pt','ru'))
);
create table public.app_feedback (
 id uuid primary key,
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 message text not null check (char_length(btrim(message)) between 10 and 2000),
 created_at timestamptz not null default now()
);
create index stickers_owner_idx on public.stickers(user_id, created_at desc);
create index collection_stickers_owner_idx on public.collection_stickers(user_id);
alter table public.stickers enable row level security;
alter table public.collection_stickers enable row level security;
alter table public.user_settings enable row level security;
alter table public.app_feedback enable row level security;
grant select, insert, update, delete on public.stickers, public.collection_stickers, public.user_settings to authenticated;
grant select, insert on public.app_feedback to authenticated;
create policy "Own stickers" on public.stickers for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "Own collection stickers" on public.collection_stickers for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "Own settings" on public.user_settings for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "Read own feedback" on public.app_feedback for select to authenticated using (user_id = auth.uid());
create policy "Submit feedback" on public.app_feedback for insert to authenticated with check (user_id = auth.uid());
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('sticker-images', 'sticker-images', false, 5242880, array['image/png']);
create policy "Read own sticker images" on storage.objects for select to authenticated
using (bucket_id = 'sticker-images' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "Upload own sticker images" on storage.objects for insert to authenticated
with check (bucket_id = 'sticker-images' and (storage.foldername(name))[1] = auth.uid()::text);


-- Bundled catalogue identifiers are shared across Home, Search, Category and Studio.
create table public.sticker_favorites (
 user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
 sticker_id text not null check (sticker_id ~ '^(trending|frame|animal|hug)-[0-2]$'),
 created_at timestamptz not null default now(),
 primary key (user_id, sticker_id)
);
alter table public.sticker_favorites enable row level security;
grant select, insert, update, delete on public.sticker_favorites to authenticated;
create policy "Own favorite stickers" on public.sticker_favorites for all to authenticated
using (user_id = auth.uid()) with check (user_id = auth.uid());

commit;
