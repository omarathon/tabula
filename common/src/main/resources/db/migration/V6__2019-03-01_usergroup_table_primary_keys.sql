delete from usergroupinclude where usercode is null;
delete from usergroupexclude where usercode is null;
delete from usergroupstatic where usercode is null;

alter table usergroupinclude alter column usercode set not null;
alter table usergroupexclude alter column usercode set not null;
alter table usergroupstatic alter column usercode set not null;

alter table usergroupinclude add constraint pk_usergroupinclude primary key (group_id, usercode);
alter table usergroupexclude add constraint pk_usergroupexclude primary key (group_id, usercode);
alter table usergroupstatic add constraint pk_usergroupstatic primary key (group_id, usercode);
