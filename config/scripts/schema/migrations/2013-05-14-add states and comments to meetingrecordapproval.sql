alter table meetingrecordapproval drop column approved;
alter table meetingrecordapproval drop column lastupdateddate;

alter table meetingrecordapproval add (
	comments NCLOB,
	approval_state NVARCHAR2(50),
	last_updated_date TIMESTAMP,
	creation_date TIMESTAMP
);

