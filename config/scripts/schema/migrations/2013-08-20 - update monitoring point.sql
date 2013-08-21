alter table monitoringpointset
	modify year number(4,0) null;

alter table monitoringpointset
	add academicyear number(4,0) default '2013' not null;