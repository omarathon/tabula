-- TAB-579
create table studentcourseyeardetails (
	id nvarchar2(250) not null,
	scjcode nvarchar2(20) not null,
	scesequencenumber nvarchar2(4) not null,
    	academicYear number(4,0) not null,
	enrolmentstatuscode nvarchar2(10),
	modeofattendancecode nvarchar2(10),
	yearofstudy nvarchar2(2),
	lastupdateddate timestamp(6),
	hib_version number,
	constraint studentcourseyeardetails_pk primary key(id)
);
