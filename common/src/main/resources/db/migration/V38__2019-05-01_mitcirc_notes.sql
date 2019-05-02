create table mitigatingcircumstancesnote (
  id varchar(255) not null,
  submission_id varchar(255) not null,
  text bytea not null,
  creator varchar(255) not null,
  createdDate timestamp(6) not null,
  lastModifiedBy varchar(255) not null,
  lastModifiedDate timestamp(6) not null,
  constraint pk_mitigatingcircumstancesnote primary key (id),
  constraint fk_mitigatingcircumstancesnote_submission foreign key (submission_id) references mitigatingcircumstancessubmission (id)
);

create index idx_mitigatingcircumstancesnote_submission on mitigatingcircumstancesnote (submission_id);

create table mitcircsnoteattachment (
  note_id varchar(255) not null,
  file_attachment_id varchar(255) not null,
  constraint pk_mitcircsnoteattachment primary key (note_id, file_attachment_id),
  constraint fk_mitcircsnoteattachment_message foreign key (note_id) references mitigatingcircumstancesnote (id),
  constraint fk_mitcircsnoteattachment_file foreign key (file_attachment_id) references fileattachment (id)
);

create index idx_mitcircssubmissionnote_attachment on mitcircsnoteattachment (file_attachment_id);
create index idx_mitcircssubmissionnote_message on mitcircsnoteattachment (note_id);
