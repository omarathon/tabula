create index idx_stagemarkers_markers on stagemarkers (markers);

-- Fix indices against wrong table
drop index idx_usergroupstatic_group;
drop index idx_usergroupstatic_uid;
create index idx_usergroupstatic_group on usergroupstatic (group_id);
create index idx_usergroupstatic_uid on usergroupstatic (usercode);