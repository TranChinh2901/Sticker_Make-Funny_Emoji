-- Run only in an isolated PostgreSQL database after setup.sql and Supabase auth/storage stubs.
insert into auth.users(id) values ('00000000-0000-0000-0000-000000000001'), ('00000000-0000-0000-0000-000000000002');
set role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', false);
insert into public.sticker_collections(id,name) values ('10000000-0000-0000-0000-000000000001','Owner one');
insert into public.stickers(id,name,image_path) values ('20000000-0000-0000-0000-000000000001','One','00000000-0000-0000-0000-000000000001/one.png');
insert into public.collection_stickers(collection_id,sticker_id) values ('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001');
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', false);
do $$ begin
 if (select count(*) from public.stickers) != 0 then raise exception 'Other user can read stickers'; end if;
 if (select count(*) from public.sticker_collections) != 0 then raise exception 'Other user can read collections'; end if;
 if (select count(*) from public.collection_stickers) != 0 then raise exception 'Other user can read links'; end if;
end $$;
insert into public.stickers(id,name,image_path) values ('20000000-0000-0000-0000-000000000002','Two','00000000-0000-0000-0000-000000000002/two.png');
do $$ begin
 begin
  insert into public.collection_stickers(collection_id,sticker_id) values ('10000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002');
  raise exception 'Cross-owner link allowed';
 exception when foreign_key_violation then null; end;
 begin
  insert into public.stickers(id,user_id,name,image_path) values ('20000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001','Impersonation','00000000-0000-0000-0000-000000000001/evil.png');
  raise exception 'Impersonation allowed';
 exception when insufficient_privilege then null; end;
 begin
  insert into public.stickers(id,name,image_path) values ('20000000-0000-0000-0000-000000000004','Wrong path','00000000-0000-0000-0000-000000000001/evil.png');
  raise exception 'Foreign storage path allowed';
 exception when check_violation then null; end;
end $$;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', false);
delete from public.sticker_collections where id = '10000000-0000-0000-0000-000000000001';
do $$ begin
 if (select count(*) from public.collection_stickers) != 0 then raise exception 'Link not cascaded'; end if;
 if (select count(*) from public.stickers) != 1 then raise exception 'Deleting collection deleted sticker'; end if;
end $$;
reset role;
select 'PASS: owner isolation, cross-owner FK, impersonation, path ownership, collection-only deletion';
