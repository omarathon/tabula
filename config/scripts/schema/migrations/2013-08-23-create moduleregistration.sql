create table moduleregistration (
  id nvarchar2(250) not null,
  sprcode nvarchar2(20) not null,
  modulecode nvarchar2(20) not null,
  academicyear number(4,0) not null,
  cats number(5,2),
  assessmentgroup nvarchar2(2),
  selectionstatuscode nvarchar2(6),
  lastupdateddate timestamp(6),
  hib_version number
);

create unique index idx_moduleregistration_notional_key on moduleregistration(sprcode, modulecode, academicyear, cats);

alter table studentcourseyeardetails add modregstatus nvarchar2(10);

