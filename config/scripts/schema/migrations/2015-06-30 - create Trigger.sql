-- TAB-3405
CREATE TABLE SCHEDULEDTRIGGER
(
    ID NVARCHAR2(255) NOT NULL
  , TRIGGER_TYPE NVARCHAR2(255) NOT NULL
  , SCHEDULED_DATE TIMESTAMP NOT NULL
  , TARGET_ID NVARCHAR2(255)
  , COMPLETED_DATE TIMESTAMP
  , CONSTRAINT SCHEDULEDTRIGGER_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_TRIGGER_TYPE ON SCHEDULEDTRIGGER (TRIGGER_TYPE);

ALTER TABLE FEEDBACK MODIFY (UPLOADERID NULL);