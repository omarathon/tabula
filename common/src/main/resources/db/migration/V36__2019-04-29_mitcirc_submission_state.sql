alter table mitigatingcircumstancessubmission add column state varchar;
update mitigatingcircumstancessubmission set state = 'Draft' where approvedon is null;
update mitigatingcircumstancessubmission set state = 'Submitted' where approvedon is not null;
alter table mitigatingcircumstancessubmission alter column state set not null;
create index idx_mitcircssubmission_state on mitigatingcircumstancessubmission (state);
