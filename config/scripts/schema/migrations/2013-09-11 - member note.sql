CREATE TABLE membernote (

      	ID NVARCHAR2(255) NOT NULL,
      	MEMBERID NVARCHAR2(255) NOT NULL,
        NOTE NCLOB,
        TITLE NVARCHAR2(500),
        CREATORID NVARCHAR2(255) NOT NULL,
      	CREATION_DATE TIMESTAMP NOT NULL,
      	LAST_UPDATED_DATE TIMESTAMP NOT NULL,

        CONSTRAINT "MEMBERNOTE_PK" PRIMARY KEY ("ID")
      );

      CREATE INDEX IDX_NOTE_MEMBER ON MEMBERNOTE ("MEMBERID");

alter table fileattachment
add member_note_id nvarchar2(255);

create index fileattachment_membernote on fileattachment(member_note_id);