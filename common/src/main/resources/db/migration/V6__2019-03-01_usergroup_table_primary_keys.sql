delete
from usergroupinclude
where usercode is null;
delete
from usergroupexclude
where usercode is null;
delete
from usergroupstatic
where usercode is null;

delete
from usergroupinclude ug1 using usergroupinclude ug2
where ug1.ctid < ug2.ctid
  and ug1.group_id = ug2.group_id
  and ug1.usercode = ug2.usercode;

delete
from usergroupexclude ug1 using usergroupexclude ug2
where ug1.ctid < ug2.ctid
  and ug1.group_id = ug2.group_id
  and ug1.usercode = ug2.usercode;

delete
from usergroupstatic ug1 using usergroupstatic ug2
where ug1.ctid < ug2.ctid
  and ug1.group_id = ug2.group_id
  and ug1.usercode = ug2.usercode;

alter table usergroupinclude
  alter column usercode set not null;
alter table usergroupexclude
  alter column usercode set not null;
alter table usergroupstatic
  alter column usercode set not null;

alter table usergroupinclude
  add constraint pk_usergroupinclude primary key (group_id, usercode);
alter table usergroupexclude
  add constraint pk_usergroupexclude primary key (group_id, usercode);
alter table usergroupstatic
  add constraint pk_usergroupstatic primary key (group_id, usercode);
