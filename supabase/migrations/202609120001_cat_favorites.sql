-- Search fixtures are bundled in the app. Only favorites are persisted remotely.
begin;
alter table public.sticker_favorites drop constraint sticker_favorites_sticker_id_check;
alter table public.sticker_favorites add constraint sticker_favorites_sticker_id_check
  check (sticker_id ~ '^(trending|frame|animal|hug)-[0-2]$' or sticker_id ~ '^cat-[0-8]$');
commit;
