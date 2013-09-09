alter table monitoringpointset
	modify (
		ROUTE_ID NVARCHAR2(250) null,
		ACADEMICYEAR NUMBER(4,0) null
	);

alter table monitoringpointset
	add (
		DISCRIMINATOR NVARCHAR2(100) default 'normal' not null,
		POSITION NUMBER(3,0) null
	);