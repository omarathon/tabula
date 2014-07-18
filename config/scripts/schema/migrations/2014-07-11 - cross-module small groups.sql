-- TAB-2450
create table DepartmentSmallGroupSet (
  id nvarchar2(255) not null,
  department_id nvarchar2(255) not null,
  academicYear number(4,0) not null,
  name nvarchar2(255),
  archived number(1, 0) default 0,
  deleted number(1, 0) default 0,
  membersgroup_id nvarchar2(255),
  member_query nclob,
  constraint DSmallGroupSet_PK primary key (id)
);

create index idx_dsmallgroupset_dept on DepartmentSmallGroupSet(department_id);
create index idx_dsmallgroupset_deleted on DepartmentSmallGroupSet(deleted);

create table DepartmentSmallGroup (
  id nvarchar2(255) not null,
  set_id nvarchar2(255),
  name nvarchar2(255),
  deleted number(1, 0) default 0,
  studentsgroup_id nvarchar2(255),
  constraint DSmallGroup_PK primary key (id)
);

create index idx_dsmallgroup_set on DepartmentSmallGroup(set_id);
create index idx_dsmallgroup_deleted on DepartmentSmallGroup(deleted);