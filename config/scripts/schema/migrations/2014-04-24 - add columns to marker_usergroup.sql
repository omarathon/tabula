-- TAB-1922

alter table marker_usergroup add (discriminator VARCHAR2(100) default 'first' not null);
alter table marker_usergroup add (id VARCHAR2(255));

-- Post-deploy

update marker_usergroup set id = ASSIGNMENT_ID || '-' || marker_uni_id || '-' || discriminator;
alter table marker_usergroup modify (id VARCHAR2(255) not null);
alter table marker_usergroup add CONSTRAINT marker_usergroup_PK PRIMARY KEY (ID);

-- All markers will be first markers (using the default discriminator)
-- Set second markers as appropriate
update MARKER_USERGROUP set DISCRIMINATOR = 'second' where ID in (
  select MARKER_USERGROUP.ID from MARKER_USERGROUP
    join ASSIGNMENT on ASSIGNMENT.ID = MARKER_USERGROUP.ASSIGNMENT_ID
    join MARKSCHEME on MARKSCHEME.ID = ASSIGNMENT.MARKSCHEME_ID
    join USERGROUPINCLUDE on USERGROUPINCLUDE.GROUP_ID = MARKSCHEME.SECONDMARKERS_ID
      and MARKER_USERGROUP.MARKER_UNI_ID = USERGROUPINCLUDE.USERCODE
);