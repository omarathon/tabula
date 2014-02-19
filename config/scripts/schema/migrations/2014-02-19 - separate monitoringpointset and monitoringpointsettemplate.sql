--- TAB-1963

--- Create new tables for MonitoringPointSetTemplate and MonitoringPointTemplate
CREATE TABLE MONITORINGPOINTSETTEMPLATE
(	"ID" NVARCHAR2(250) NOT NULL,
  "CREATEDDATE" TIMESTAMP (6),
  "UPDATEDDATE" TIMESTAMP (6),
  "TEMPLATENAME" NVARCHAR2(255) NOT NULL,
  "POSITION" NUMBER(3,0),
  CONSTRAINT "PK_MPSTEMPLATE" PRIMARY KEY ("ID")
);

CREATE UNIQUE INDEX IDX_MPST_TEMPLATE_NAME ON MONITORINGPOINTSETTEMPLATE ("TEMPLATENAME");

CREATE TABLE MONITORINGPOINTTEMPLATE
(	"ID" NVARCHAR2(250) NOT NULL,
  "POINT_SET_ID" NVARCHAR2(250) NOT NULL,
  "NAME" NVARCHAR2(4000) NOT NULL,
  "DEFAULTVALUE" NUMBER(1,0) DEFAULT 0,
  "CREATEDDATE" TIMESTAMP (6),
  "UPDATEDDATE" TIMESTAMP (6),
  "WEEK" NUMBER(2,0),
  "VALIDFROMWEEK" NUMBER(2,0) DEFAULT '0' NOT NULL ENABLE,
  "REQUIREDFROMWEEK" NUMBER(2,0) DEFAULT '0' NOT NULL ENABLE,
  "SETTINGS" NCLOB,
  "POINT_TYPE" NVARCHAR2(50),
  CONSTRAINT "PK_MPTEMPLATE" PRIMARY KEY ("ID")
);

CREATE INDEX IDX_MPTEMPLATE_POINTSETID ON MONITORINGPOINTTEMPLATE ("POINT_SET_ID");

--- Migrate any existing data
insert into monitoringpointsettemplate
  select id, createddate, updateddate, templatename, position from monitoringpointset where discriminator = 'template';
delete from monitoringpointset where discriminator = 'template';

insert into monitoringpointtemplate
  select id, point_set_id, name, defaultvalue, createddate, updateddate, week, validfromweek, requiredfromweek, settings, point_type from monitoringpoint where point_set_id in (select id from monitoringpointsettemplate);
delete from monitoringpoint where point_set_id in (select id from monitoringpointsettemplate);

--- Mark old columns as unused
alter table MonitoringPointSet set unused column templatename;
alter table MonitoringPointSet set unused column discriminator;
alter table MonitoringPointSet set unused column position;
alter table MonitoringPointSet drop unused columns;

--- Recreate index on MonitoringPointSet now that TemplateName no longer exists
create unique index IDX_MPSY_ROUTE_YEAR on MonitoringPointSet (route_id, year, academicyear);