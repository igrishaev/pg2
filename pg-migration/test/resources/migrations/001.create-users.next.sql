
create table IF NOT EXISTS test_users (id serial primary key, name text not null);

begin;

insert into test_users (name) values ('Ivan');
insert into test_users (name) values ('Huan');
insert into test_users (name) values ('Juan');

commit;
