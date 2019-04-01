-- fix past crimes - shouldn't be a unique index
drop index idx_mitcircssub_student;
create index idx_mitcircssub_student on MitigatingCircumstancesSubmission (universityId);

create table mitcircssubmissionattachment (
  submission_id varchar(255) not null,
  file_attachment_id varchar(255) not null,
  constraint pk_mitcircssubmissionattachment primary key (submission_id, file_attachment_id)
);

create index idx_mitcircssubmission_attachment on mitcircssubmissionattachment (file_attachment_id);
create index idx_mitcircssubmission_submission on mitcircssubmissionattachment (submission_id);


