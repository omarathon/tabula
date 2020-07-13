create table classification (
	code varchar(20) not null,
	shortname varchar(20),
	name varchar(300),
	lastupdateddate timestamp (6),
	constraint pk_classification primary key (code)
);
