-- TAB-4395
alter table ORIGINALITYREPORT add (
  nextSubmitAttempt TIMESTAMP,
  submitAttempts NUMBER(2) DEFAULT 0,
  submittedDate TIMESTAMP,
  nextResponseAttempt TIMESTAMP,
  responseAttempts NUMBER(2) DEFAULT 0,
  responseReceived TIMESTAMP,
  reportUrl nvarchar2(255),
  significance FLOAT(5),
  matchCount NUMBER(2),
  sourceCount NUMBER(2)
);