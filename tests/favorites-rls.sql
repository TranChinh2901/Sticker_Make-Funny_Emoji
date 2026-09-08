-- Isolated PostgreSQL only, after setup.sql and studio-rls.sql.
set role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', false);
insert into public.sticker_favorites(sticker_id) values ('trending-0');
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', false);
do $$ begin
 if (select count(*) from public.sticker_favorites) != 0 then raise exception 'Favorite leaked'; end if;
 begin
  insert into public.sticker_favorites(user_id,sticker_id) values ('00000000-0000-0000-0000-000000000001','frame-0');
  raise exception 'Favorite impersonation allowed';
 exception when insufficient_privilege then null; end;
end $$;
delete from public.sticker_favorites where sticker_id = 'trending-0';
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', false);
do $$ begin
 if (select count(*) from public.sticker_favorites) != 1 then raise exception 'Other user deleted favorite'; end if;
end $$;
insert into public.sticker_favorites(sticker_id) values ('trending-0') on conflict (user_id,sticker_id) do update set sticker_id = excluded.sticker_id;
delete from public.sticker_favorites where sticker_id = 'trending-0';
do $$ begin
 if (select count(*) from public.sticker_favorites) != 0 then raise exception 'Unfavorite failed'; end if;
end $$;
reset role;
select 'PASS: private favorites, idempotent save, owner-only delete';
