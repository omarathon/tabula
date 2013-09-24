alter table monitoringpoint
	add (
		validfromweek number(2,0) default '0' not null,
		requiredfromweek number(2,0) default '0' not null
	);

update monitoringpoint set validfromweek = week, requiredfromweek = week;

alter table monitoringcheckpoint
	add (
		state NVARCHAR2(20) default 'attended' not null,
		updateddate TIMESTAMP default sysdate not null,
    	updatedby NVARCHAR2(255) default '' not null
	);

update monitoringcheckpoint set state = 'unauthorised' where checked = 0;
update monitoringcheckpoint set updateddate = createddate, updatedby = createdby;

alter table monitoringcheckpoint
	modify (
		CREATEDBY NVARCHAR2(255) null
	);