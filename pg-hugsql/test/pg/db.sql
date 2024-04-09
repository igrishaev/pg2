
-- :name try-select-jsonb :? :*
select '{"foo": 42}'::jsonb as json

-- :name create-test-table :!
create temp table :i:table (id serial primary key, title text not null);

-- :name insert-into-table :! :n
insert into :i:table (title) values (:title);

-- :name select-from-table :? :*
select * from :i:table order by id;

-- :name get-by-id :? :1
select * from :i:table where id = :id limit 1;
