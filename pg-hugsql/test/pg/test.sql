
-- :name try-select-jsonb :? :*
select '{"foo": 42}'::jsonb as json

-- :name create-test-table :!
create temp table :i:table (id serial primary key, title text not null);

-- :name insert-into-table :! :n
insert into :i:table (title) values (:title);

-- :name insert-into-table-ret :<!
insert into :i:table (title) values (:title) returning *;

-- :name select-from-table :? :*
select * from :i:table order by id;

-- :name get-by-id :? :1
select * from :i:table where id = :id limit 1;

-- :name select-json-from-param :? :1
select :json::json as json;

-- :name select-value-list :? :*
select * from :i:table where id in (:v*:ids) order by id;

-- :name select-tuple-param :? :*
select * from :i:table where (id, title) = :tuple:pair;

-- :name insert-tuple-list :<!
insert into :i:table (id, title) values :t*:rows returning *;

-- :name select-identifiers-list :? :*
select :i*:fields from :i:table order by :i:order-by;
