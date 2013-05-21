create table SmallGroupSet (
	id nvarchar2(255) not null,
	module_id nvarchar2(255) not null,
	academicYear number(4,0) not null,
	name nvarchar2(255),
	archived number(1, 0) default 0,
	deleted number(1, 0) default 0,
	group_format nvarchar2(255) not null,
	constraint SmallGroupSet_PK primary key (id)
);
   
create index idx_smallgroupset_module on SmallGroup(module_id);
create index idx_smallgroupset_deleted on SmallGroup(deleted);

create table SmallGroup (
	id nvarchar2(255) not null,
	set_id nvarchar2(255) not null,
	name nvarchar2(255),
	deleted number(1, 0) default 0,
	constraint SmallGroup_PK primary key (id)
);
   
create index idx_smallgroup_set on SmallGroup(set_id);
create index idx_smallgroup_deleted on SmallGroup(deleted);

create table SmallGroupEvent (
	id nvarchar2(255) not null,
	group_id nvarchar2(255) not null,
	weekRanges nvarchar2(255),
	day number not null,
	startTime nvarchar2(255) not null,
	endTime nvarchar2(255) not null,
	title nvarchar2(255),
	location nvarchar2(255),	
	constraint SmallGroupEvent_PK primary key (id)
);

create index idx_smallgroupevent_group on SmallGroupEvent(group_id);