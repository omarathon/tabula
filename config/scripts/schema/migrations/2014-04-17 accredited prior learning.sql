-- TAB-2112 AccreditedPriorLearning table

create table accreditedpriorlearning (
  id nvarchar2(250) not null,
  scjcode nvarchar2(20) not null,
  awardcode nvarchar2(20) not null,
  sequencenumber nvarchar2(10) not null,
  academicyear number(4,0) not null,
  cats number(5,2),
  levelcode nvarchar2(20),
  reason nvarchar2(200),
  lastupdateddate timestamp(6),
  hib_version number
);

create unique index idx_apl_notional_key on accreditedpriorlearning(scjcode, awardcode, sequencenumber);
