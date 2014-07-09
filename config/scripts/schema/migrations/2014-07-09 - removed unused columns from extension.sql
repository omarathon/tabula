-- TAB-1921
alter table Extension set unused column Approved;
alter table Extension set unused column Rejected;
alter table Extension drop unused columns;