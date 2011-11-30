create table auditevent (
	eventdate timestamp,
	eventType varchar(255) not null,
	eventStage varchar(64) not null,
	real_user_id varchar(255) not null,
	masquerade_user_id varchar(255) not null,
	data varchar(4000) not null
);