-- TAB-2978
alter table Member add deceased number(1,0);
update Member set deceased = 1 where universityid in (select universityid from studentcoursedetails where sprstatuscode = 'D' or scjstatuscode = 'D');
update Member set deceased = 0 where deceased is null;
alter table Member modify deceased default 0;