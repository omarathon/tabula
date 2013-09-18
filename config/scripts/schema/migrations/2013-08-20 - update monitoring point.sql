alter table monitoringpointset
	modify year number(4,0) null;

alter table monitoringpointset
	add academicyear number(4,0) default '0' not null;