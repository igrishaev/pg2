
-- :name create-demo-table :!
create table :i:table (id serial primary key, title text not null);

-- :name insert-into-table :! :n
insert into :i:table (title) values (:title);

-- :name insert-into-table-returning :<!
insert into :i:table (title) values (:title) returning *;

-- :name select-from-table :? :*
select * from :i:table order by id;

-- :name get-by-id :? :1
select * from :i:table where id = :id limit 1;

-- :name get-by-ids :? :*
select * from :i:table where id in (:v*:ids) order by id;

-- :name insert-rows :<!
insert into :i:table (id, title) values :t*:rows returning *;

-- :name update-title-by-id :<!
update :i:table set title = :title where id = :id returning *;

-- :name delete-from-tablee :n
delete from :i:table;

-- :name try-select-jsonb :? :*
select '{"foo": 42}'::jsonb as json
