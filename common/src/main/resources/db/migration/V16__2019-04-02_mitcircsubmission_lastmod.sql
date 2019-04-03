alter table mitigatingcircumstancessubmission add column lastmodified timestamp(6);
update mitigatingcircumstancessubmission set lastmodified = createddate;
alter table mitigatingcircumstancessubmission alter column lastmodified set not null;