create table meetingrecordapproval (
   id nvarchar2(255) not null,
   meetingrecord_id nvarchar2(255) not null,
   approver_id nvarchar2(255) not null,
   approved NUMBER(1,0) DEFAULT 0,
   lastupdateddate timestamp
   );
   
create index idx_meetingapproval_record on meetingrecordapproval("MEETINGRECORD_ID");
create index idx_meetingapproval_member on meetingrecordapproval("APPROVER_ID");
