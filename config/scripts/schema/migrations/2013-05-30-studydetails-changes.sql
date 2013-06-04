-- the first line (add a column) needs to be executed before deploy, 
-- the second (make a column unused) after deploy.

alter table studydetails add scjcode varchar2(15);

-- this effectively deletes the column forever, so don't rush into it:
alter table studydetails set unused ugpg;

