alter table monitoringpointset
	drop column senttoacademicoffice;

alter table monitoringpoint
	add senttoacademicoffice number(1) default 0;

drop index idx_mpsy_route_year;

create unique index idx_mpsy_route_year on monitoringpointset(route_id, year, academicyear, templatename);