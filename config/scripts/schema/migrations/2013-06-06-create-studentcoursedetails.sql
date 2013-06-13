create table studentcoursedetails (
	scjcode nvarchar2(20) not null,
	universityid nvarchar2(20) not null,
	sprcode nvarchar2(20) not null,
	routecode nvarchar2(20),
	coursecode nvarchar2(20),
	routedept nvarchar2(20),
	awardcode nvarchar2(20),
	sprstatus nvarchar2(12),
	levelcode nvarchar2(10),
	begindate date,
	enddate date,
	expectedenddate date,
	courseyearlength number(4),
	hib_version number,
	constraint studentcoursedetails_pk primary key (scjcode)
	constraint sprcode UNIQUE (sprcode);
);

