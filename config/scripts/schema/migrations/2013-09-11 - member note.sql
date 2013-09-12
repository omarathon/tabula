create table membernote (

      	id nvarchar2(255) not null,
      	memberid nvarchar2(255) not null,
        note nclob,
        title nvarchar2(500),
        creatorid nvarchar2(255) not null,
      	creationdate timestamp not null,
      	lastupdateddate timestamp not null,

        constraint "membernote_pk" primary key ("id")
      );

create index idx_note_member on membernote (memberid);

alter table fileattachment
add member_note_id nvarchar2(255);

create index fileattachment_membernote on fileattachment(member_note_id);