CREATE TABLE MEETINGRECORDRELATIONSHIP (
  MEETING_RECORD_ID nvarchar2(255) not null,
  RELATIONSHIP_ID   nvarchar2(255) not null,

  constraint "MEETINGRECORDRELATIONSHIP_PK" primary key (MEETING_RECORD_ID, RELATIONSHIP_ID)
);

create index idx_meetingrecordrelationship
  on MEETINGRECORDRELATIONSHIP (MEETING_RECORD_ID);

alter table MEETINGRECORD
  modify (relationship_id null);
