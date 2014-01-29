--- TAB-128

alter table member add disability nvarchar2(20);

create table disability
(	code nvarchar2(20) not null enable,
	shortName nvarchar2(20),
	definition nvarchar2(300),
	lastUpdatedDate timestamp(6),
  constraint disability_pk primary key (code)
);