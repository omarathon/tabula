create table fileattachmenttoken (
  ID NVARCHAR2(255) NOT NULL,
  EXPIRES TIMESTAMP,
  USED number(1,0) default 0 not null,
  FILEATTACHMENT_ID nvarchar2(255)
);