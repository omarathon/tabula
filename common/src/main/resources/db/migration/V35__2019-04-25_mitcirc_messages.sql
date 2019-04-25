create table mitigatingcircumstancesmessage (
  id varchar(255) not null primary key,
  submission_id varchar(255) not null,
  message bytea not null,
  sender varchar(255) not null,
  createdDate timestamp(6) not null,
  constraint pk_mitigatingcircumstancesmessage primary key (id),
  constraint fk_mitigatingcircumstancesmessage_submission foreign key (submission_id) references mitigatingcircumstancessubmission (id)
);

create table mitcircssmessageattachment (
  message_id varchar(255) not null,
  file_attachment_id varchar(255) not null,
  constraint pk_mitcircssmessageattachment primary key (message_id, file_attachment_id),
  constraint fk_mitcircssmessageattachment_message foreign key (message_id) references mitigatingcircumstancesmessage (id),
  constraint fk_mitcircssmessageattachment_file foreign key (file_attachment_id) references fileattachment (id)
);

create index idx_mitcircssubmissionmessage_attachment on mitcircssmessageattachment (file_attachment_id);
create index idx_mitcircssubmissionmessage_message on mitcircssmessageattachment (message_id);