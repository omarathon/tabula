create table SmallGroupEventAttendance (
	id nvarchar2(250) not null,
	occurrence_id nvarchar2(250) not null,
	universityId nvarchar2(250) not null,
	state NVARCHAR2(20) default 'attended' not null,
	updateddate TIMESTAMP default sysdate not null,
  updatedby NVARCHAR2(255) not null,
	constraint sgattendance_pk primary key (id)
);

create index idx_sgattendance_occurrence on SmallGroupEventAttendance(occurrence_id);
create index idx_sgattendance_uni_id on SmallGroupEventAttendance(universityId);
create unique index idx_sgattendance_ck on SmallGroupEventAttendance(occurrence_id, universityId);
alter table smallgroupeventoccurrence drop constraint sgeo_group_fk;

-- migrate existing attended
insert into smallgroupeventattendance (id, occurrence_id, universityid, state, updatedby, updateddate)
(select occurrence.id || '_' || uci.usercode as id, occurrence.id as occurrence_id, uci.usercode as universityid, 'attended' as state, 'tabula' as updatedby, sysdate as updateddate
  from smallgroupeventoccurrence occurrence
    join usergroupinclude uci on uci.group_id = occurrence.membersgroup_id);
    
-- migrate existing missed to missed unauthorised
insert into smallgroupeventattendance (id, occurrence_id, universityid, state, updatedby, updateddate)
(select o.id || '_' || student.usercode as id, o.id as occurrence_id, student.usercode as universityid, 'unauthorised' as state, 'tabula' as updatedby, sysdate as updateddate
  from smallgroup g
  join smallgroupevent e on g.id = e.group_id
  join smallgroupeventoccurrence o on e.id = o.event_id
  join usergroupinclude student on student.group_id = g.studentsgroup_id
  left outer join usergroupinclude attended on attended.group_id = o.membersgroup_id and attended.usercode = student.usercode
  where attended.usercode is null);

-- WARNING Don't run this on live unless you know what you're doing and TAB-1650 has been deployed
/* 
delete from usergroupinclude where group_id in (select membersgroup_id from smallgroupeventoccurrence);
delete from usergroup where id in (select membersgroup_id from smallgroupeventoccurrence);
update smallgroupeventoccurrence set membersgroup_id = null;
alter table smallgroupeventoccurrence set unused column membersgroup_id;
alter table smallgroupeventoccurrence drop unused columns;
*/