
insert into usergroup (id) values ('1');
insert into usergroupinclude (group_id, usercode) values ('1', 'cusebr')
insert into usergroupinclude (group_id, usercode) values ('1', 'cusfal')

insert into department (id,code,name,ownersgroup_id) values ('1','CS','Computer Science','1');
insert into department (id,code,name) values ('2','CH','Chemistry');

insert into module (id,department_id,code,name,active) values ('1','1','CS108','Introduction to Programming',1);
insert into module (id,department_id,code,name,active) values ('2','1','CS240','History of Computing',1);
