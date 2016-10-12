-- TAB-4557

CREATE TABLE UPSTREAMASSESSMENTGROUPMEMBER (
  ID NVARCHAR2(255) NOT NULL,
  GROUP_ID NVARCHAR2(255) NOT NULL,
  universityId NVARCHAR2(250) not null,
  POSITION NUMBER(5, 0),
  ACTUALMARK NUMBER(5),
  ACTUALGRADE NVARCHAR2(255),
  AGREEDMARK NUMBER(5),
  AGREEDGRADE NVARCHAR2(255),
  CONSTRAINT UAGM_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_UAGM_GROUP ON UPSTREAMASSESSMENTGROUPMEMBER(GROUP_ID);

-- Migrate existing members
INSERT INTO UPSTREAMASSESSMENTGROUPMEMBER (ID, GROUP_ID, UNIVERSITYID, POSITION)
  SELECT SYS_GUID(), UPSTREAMASSESSMENTGROUP.ID, USERCODE, POSITION FROM UPSTREAMASSESSMENTGROUP
    JOIN USERGROUPSTATIC ON GROUP_ID = MEMBERSGROUP_ID;

-- POST DEPLOY

DELETE FROM USERGROUPSTATIC WHERE GROUP_ID IN (SELECT MEMBERSGROUP_ID FROM UPSTREAMASSESSMENTGROUP);
ALTER TABLE USERGROUPSTATIC DROP CONSTRAINT USERGROUPSTATIC_PK;
ALTER TABLE USERGROUPSTATIC SET UNUSED COLUMN ID;
ALTER TABLE USERGROUPSTATIC SET UNUSED COLUMN POSITION;
ALTER TABLE USERGROUPSTATIC DROP UNUSED COLUMNS;
ALTER TABLE UPSTREAMASSESSMENTGROUP SET UNUSED COLUMN MEMBERSGROUP_ID;
ALTER TABLE UPSTREAMASSESSMENTGROUP DROP UNUSED COLUMNS;