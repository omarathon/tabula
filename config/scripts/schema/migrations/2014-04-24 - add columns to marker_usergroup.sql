-- TAB-1922

alter table marker_usergroup add (discriminator VARCHAR2(100) default 'first' not null);
alter table marker_usergroup add (id VARCHAR2(255));

-- Post-deploy

update marker_usergroup set id = ASSIGNMENT_ID || '-' || marker_uni_id || '-' || discriminator;
alter table marker_usergroup modify (id VARCHAR2(255) not null);
alter table marker_usergroup add CONSTRAINT marker_usergroup_PK PRIMARY KEY (ID);