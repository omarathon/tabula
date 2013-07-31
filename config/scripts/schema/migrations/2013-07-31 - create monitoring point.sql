-- TAB-992

create table monitoringpointsetyear (
	id nvarchar2(250) not null,
	route_id nvarchar2(250) not null,
	year number(4,0) not null
	senttoacademicoffice number(1) default 0
	constraint monitoringpointsetyear_pk primary key(id),
	constraint mpsy_unique unique (route_id, year)
);

create table monitoringpointset (
	id nvarchar2(250) not null,
	point_set_year_id nvarchar2(250) not null,
	name nvarchar2(4000) not null
	position number(2,0) not null default 0
	defaultvalue number(1) default 0
	createddate timestamp(6),
	updateddate timestamp(6),
	constraint monitoringpointset_pk primary key(id)
);

create table monitoringpoint (
	id nvarchar2(250) not null,
	point_set_id nvarchar2(250) not null,
	name nvarchar2(4000) not null
	position number(2,0) not null default 0
	createddate timestamp(6),
	updateddate timestamp(6),
	constraint monitoringpoint_pk primary key(id)
);

create table monitoringcheckpoint (
	id nvarchar2(250) not null,
	point_id nvarchar2(250) not null,
	student_course_detail_id nvarchar2(250) not null,
	checked number(1) default 0
	createddate timestamp(6),
	createdby nvarchar2(255) not null
	constraint checkpoint_pk primary key(id)
);
