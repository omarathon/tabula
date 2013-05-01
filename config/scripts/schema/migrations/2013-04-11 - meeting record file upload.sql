alter table fileattachment
add meetingrecord_id nvarchar2(255);

create index fileattachment_meetingrec on fileattachment(meetingrecord_id);