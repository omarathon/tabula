delete from mitcircssubmissionattachment;
delete from mitigatingcircumstancessubmission; -- delete all rows as department must be set
alter table mitigatingcircumstancessubmission add column department_id varchar(255) not null;
create index idx_mitcircssubmission_department on mitigatingcircumstancessubmission(department_id);
alter table mitigatingcircumstancessubmission add constraint fk_mitcircssubmission_department foreign key (department_id) references department (id);