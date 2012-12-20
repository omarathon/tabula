create table MarkerFeedback (
	id nvarchar2(255) not null,
	mark  NUMBER(5, 0),
	state nvarchar2(255),
	uploaded_date timestamp not null,
	feedback_id nvarchar2(255),
	CONSTRAINT "MARKERFEEDBACK_PK" PRIMARY KEY ("ID")
);

create table MarkerFeedbackAttachment (
	marker_feedback_id nvarchar2(255),
	file_attachment_id nvarchar2(255)
);

create index markerfeedback_fileattachment on MarkerFeedbackAttachment(marker_feedback_id, file_attachment_id);

alter table feedback add (
	first_marker_feedback nvarchar2(255),
	second_marker_feedback nvarchar2(255)
);

create index feedback_markerfeedback1 on feedback(first_marker_feedback);
create index feedback_markerfeedback2 on feedback(second_marker_feedback);

