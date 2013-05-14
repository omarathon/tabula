alter table meetingrecordapproval set unused column approved;

alter table meetingrecordapproval add (
	comments NCLOB,
	approval_state NVARCHAR2(50),
	creation_date TIMESTAMP
);

