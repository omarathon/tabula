-- TAB-4310
alter table ASSIGNMENT add (
  submitToTurnitin NUMBER(1,0) DEFAULT 0 NOT NULL,
  lastSubmittedToTurnitin TIMESTAMP,
  submitToTurnitinRetries NUMBER(2) DEFAULT 0
  );