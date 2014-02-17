CREATE TABLE NOTIFICATION
(
    ID NVARCHAR2(255) NOT NULL
  , NOTIFICATION_TYPE NVARCHAR2(255) NOT NULL
  , AGENT NVARCHAR2(255) NOT NULL
  , CREATED TIMESTAMP NOT NULL
  , TARGET_ID NVARCHAR2(255)
  , SETTINGS CLOB NOT NULL
  , RECIPIENTUSERID NVARCHAR2(255)
  , RECIPIENTUNIVERSITYID NVARCHAR2(20)
  , CONSTRAINT NOTIFICATION_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_NOTIFICATION_TYPE ON NOTIFICATION (NOTIFICATION_TYPE);

CREATE TABLE ENTITYREFERENCE
(
  ID NVARCHAR2(255) NOT NULL,
  ENTITY_TYPE NVARCHAR2(255) NOT NULL,
  ENTITY_ID NVARCHAR2(255) NOT NULL,
  NOTIFICATION_ID NVARCHAR2(255) NOT NULL,
  CONSTRAINT ENTITYREFERENCE_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_ENTITYREF_NOTIFICATION ON ENTITYREFERENCE (NOTIFICATION_ID);
