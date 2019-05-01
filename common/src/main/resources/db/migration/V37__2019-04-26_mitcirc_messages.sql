create table mitigatingcircumstancesmessage (
  id varchar(255) not null,
  submission_id varchar(255) not null,
  message bytea not null,
  sender varchar(255) not null,
  createdDate timestamp(6) not null,
  constraint pk_mitigatingcircumstancesmessage primary key (id),
  constraint fk_mitigatingcircumstancesmessage_submission foreign key (submission_id) references mitigatingcircumstancessubmission (id)
);

create index idx_mitigatingcircumstancesmessage_submission on mitigatingcircumstancesmessage (submission_id);

create table mitcircsmessageattachment (
  message_id varchar(255) not null,
  file_attachment_id varchar(255) not null,
  constraint pk_mitcircsmessageattachment primary key (message_id, file_attachment_id),
  constraint fk_mitcircsmessageattachment_message foreign key (message_id) references mitigatingcircumstancesmessage (id),
  constraint fk_mitcircsmessageattachment_file foreign key (file_attachment_id) references fileattachment (id)
);

create index idx_mitcircssubmissionmessage_attachment on mitcircsmessageattachment (file_attachment_id);
create index idx_mitcircssubmissionmessage_message on mitcircsmessageattachment (message_id);