-- TAB-579
create table studentcoursedetails (
	scjcode nvarchar2(20) not null,
	universityid nvarchar2(20) not null,
	sprcode nvarchar2(20) not null,
	routecode nvarchar2(20),
	coursecode nvarchar2(20),
	deptCode nvarchar2(20),
	awardcode nvarchar2(20),
	sprstatuscode nvarchar2(12),
	levelcode nvarchar2(10),
	begindate date,
	enddate date,
	expectedenddate date,
	courseyearlength number(4),
	mostsignificant number(1),
	lastupdateddate timestamp(6),
	hib_version number,
	constraint studentcoursedetails_pk primary key (scjcode)
	constraint sprcode UNIQUE (sprcode);
);

