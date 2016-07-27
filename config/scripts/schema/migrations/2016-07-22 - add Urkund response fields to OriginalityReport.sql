-- TAB-4438
alter table ORIGINALITYREPORT add (
  urkundResponse nclob,
  urkundResponseCode nvarchar2(100)
);