alter table mitigatingcircumstancessubmission drop column if exists issuetype;
alter table mitigatingcircumstancessubmission add column issuetypes varchar(255)[];