--- TAB-1903

alter table meetingrecord add discriminator varchar(10) default 'standard' not null ;
alter table meetingrecord add missed number(1,0) default 0;
alter table meetingrecord add missed_reason NCLOB;

create INDEX IDX_MEETINGS_MISSED on MEETINGRECORD("MISSED");
create INDEX IDX_MEETINGS_DISCRIMINATOR on MEETINGRECORD("DISCRIMINATOR");