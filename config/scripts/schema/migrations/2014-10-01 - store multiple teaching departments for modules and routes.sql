-- TAB-2589
create table ModuleTeachingInformation(
  id nvarchar2(255) not null,
  department_id nvarchar2(255) not null,
  module_id nvarchar2(255) not null,
  percentage number(6, 2),
  constraint pk_modteachinginfo primary key (id)
);

create index idx_modteachinginfo_dept on ModuleTeachingInformation(department_id);
create index idx_modteachinginfo_module on ModuleTeachingInformation(module_id);
create unique index idx_modteachinginfo_ck on ModuleTeachingInformation(department_id, module_id);
alter table ModuleTeachingInformation add constraint modteachinginfo_dept_fk foreign key (department_id) references Department;
alter table ModuleTeachingInformation add constraint modteachinginfo_mod_fk foreign key (module_id) references Module;

create table RouteTeachingInformation(
  id nvarchar2(255) not null,
  department_id nvarchar2(255) not null,
  route_id nvarchar2(255) not null,
  percentage number(6, 2),
  constraint pk_rotteachinginfo primary key (id)
);

create index idx_rotteachinginfo_dept on RouteTeachingInformation(department_id);
create index idx_rotteachinginfo_route on RouteTeachingInformation(route_id);
create unique index idx_rotteachinginfo_ck on RouteTeachingInformation(department_id, route_id);
alter table RouteTeachingInformation add constraint rotteachinginfo_dept_fk foreign key (department_id) references Department;
alter table RouteTeachingInformation add constraint rotteachinginfo_rot_fk foreign key (route_id) references Route;
