-- Extend the bundled catalogue without changing ownership policies or existing favorites.
alter table public.sticker_favorites drop constraint sticker_favorites_sticker_id_check;
alter table public.sticker_favorites add constraint sticker_favorites_sticker_id_check
  check (sticker_id ~ '^(trending|frame|animal|hug)-[0-2]$');
