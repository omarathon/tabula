-- TAB-1829

create unique index idx_ugs_groupuser on usergroupstatic (group_id, usercode);
create unique index idx_ugi_groupuser on usergroupinclude (group_id, usercode);
create unique index idx_uge_groupuser on usergroupexclude (group_id, usercode);