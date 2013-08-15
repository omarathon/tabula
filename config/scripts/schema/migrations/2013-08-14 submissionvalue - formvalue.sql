-- TAB-571

alter table SUBMISSIONVALUE modify (SUBMISSION_ID null);
alter table SUBMISSIONVALUE add FEEDBACK_ID nvarchar2(255);
alter table SUBMISSIONVALUE add MARKER_FEEDBACK_ID nvarchar2(255);
alter table FORMFIELD add CONTEXT nvarchar2(50);
update FORMFIELD set CONTEXT = 'submission';