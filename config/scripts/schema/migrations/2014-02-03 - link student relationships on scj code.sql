--- TAB-1868

alter table StudentRelationship add (
  scjCode nvarchar2(20),
  agent_type nvarchar2(20) default 'member' not null,
  external_agent_name nvarchar2(255)
);
create index StudentRelationship_scjCode on StudentRelationship (scjCode);
create index StudentRelationship_agentType on StudentRelationship (agent_type);

alter table StudentRelationship modify target_sprCode null;

--- Migrate any existing data
update StudentRelationship r
  set r.scjCode = (select max(scjCode) from StudentCourseDetails scd where scd.sprcode = r.target_sprCode)
  where r.scjCode is null;

--- Post-deploy final migrations
update StudentRelationship r
  set r.scjCode = (select max(scjCode) from StudentCourseDetails scd where scd.sprcode = r.target_sprCode)
  where r.scjCode is null;

alter table StudentRelationship modify scjCode not null;
alter table StudentRelationship set unused column target_sprCode;
alter table StudentRelationship drop unused columns;