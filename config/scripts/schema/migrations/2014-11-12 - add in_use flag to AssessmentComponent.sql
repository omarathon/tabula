-- TAB-2610
alter table UpstreamAssignment add in_use number(1, 0) default 1;
update UpstreamAssignment set in_use = 0 where name like '%NOT IN USE%';
create index UpstreamAssignment_InUse on UpstreamAssignment(in_use);

-- POST-DEPLOY ONLY
alter table UpstreamAssignment set unused column DepartmentCode;
alter table UpstreamAssignment drop unused columns;