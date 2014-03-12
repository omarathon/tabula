-- TAB-1921

alter table extension
add state nvarchar2(2) default 'U' not null;

update extension set state = 'A' where approved = 1;
update extension set state = 'R' where rejected = 1;

create index IDX_EXTENSION_STATE ON extension (state);

/* TODO should we rename approvedOn to reviewedOn, and approvalComments to reviewerComments
   TODO should we also delete approved and rejected fields... */

commit;