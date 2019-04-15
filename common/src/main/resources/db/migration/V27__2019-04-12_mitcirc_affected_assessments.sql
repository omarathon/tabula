create table mitigatingcircumstancesaffectedassessment (
  id varchar(255) not null,
  modulecode varchar(100) not null,
  module_id varchar(255) not null,
  sequence varchar(100),
  name varchar(4000) not null,
  academicyear smallint not null,
  assessmenttype varchar(32) not null,
  deadline date,
  submission_id varchar(255), -- Can't be not null because Hibernate
  constraint pk_mitigatingcircumstancesaffectedassessment primary key (id),
  constraint fk_mitigatingcircumstancesaffectedassessment_module foreign key (module_id) references module (id) on delete restrict,
  constraint fk_mitigatingcircumstancesaffectedassessment_submission foreign key (submission_id) references mitigatingcircumstancessubmission (id) on delete restrict
);

create index idx_mitigatingcircumstancesaffectedassessment_submission on mitigatingcircumstancesaffectedassessment (submission_id);
