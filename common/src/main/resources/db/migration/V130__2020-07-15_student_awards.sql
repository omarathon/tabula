create table studentaward (
  id varchar not null,
  spr_code varchar not null,
  academic_year smallint not null,
  award_code varchar(20) not null,
  classification_code varchar,
  award_date date,
  constraint pk_studentaward primary key (id)
);
create unique index ck_studentaward on studentaward (spr_code, academic_year, award_code);
