alter table monitoringpointset
	drop column senttoacademicoffice;

alter table monitoringpoint
	add senttoacademicoffice number(1) default 0;