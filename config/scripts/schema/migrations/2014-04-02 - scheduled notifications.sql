CREATE TABLE SCHEDULED_NOTIFICATION
(
    ID NVARCHAR2(255) NOT NULL
  , NOTIFICATION_TYPE NVARCHAR2(255) NOT NULL
  , SCHEDULED_DATE TIMESTAMP NOT NULL
  , TARGET_ID NVARCHAR2(255)
  , COMPLETED NUMBER(1, 0)
  , CONSTRAINT SCHEDULED_NOTIFICATION_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_S_NOTIFICATION_TYPE ON SCHEDULED_NOTIFICATION (NOTIFICATION_TYPE);


-- Make agent on notifications nullable. When scheduled notifications are resolved there will be no agent
ALTER TABLE notification MODIFY (agent NVARCHAR2(255) NULL);