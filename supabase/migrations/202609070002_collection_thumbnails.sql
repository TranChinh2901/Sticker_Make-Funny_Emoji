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
