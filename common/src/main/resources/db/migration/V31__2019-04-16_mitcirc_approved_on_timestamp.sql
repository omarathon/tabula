alter table mitigatingcircumstancessubmission
  add column approvedOn timestamp(6),
  add column lastModifiedBy varchar(255);
update mitigatingcircumstancessubmission set lastModifiedBy = creator;
alter table mitigatingcircumstancessubmission alter column lastmodifiedby set not null;