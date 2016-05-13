-- TAB-4310
alter table ORIGINALITYREPORT add (
  lastSubmittedToTurnitin TIMESTAMP,
  submitToTurnitinRetries NUMBER(2) DEFAULT 0,
  fileRequested TIMESTAMP,
  lastReportRequest TIMESTAMP,
  reportRequestRetries NUMBER(2) DEFAULT 0,
  lastTurnitinError NVARCHAR2(1000)
  );