-- TAB-3333
CREATE TABLE mark (
  id NVARCHAR2(100) NOT NULL,
  feedback_id NVARCHAR2(100) NOT NULL,
  uploaderId nvarchar2(255) not null,
  uploadedDate timestamp(6) not null,
  marktype NVARCHAR2(100) NOT NULL,
  mark number(5, 0) not null,
  grade nvarchar2(255),
  reason nvarchar2(600),
  comments nclob,
  CONSTRAINT "MARK_PK" PRIMARY KEY ("ID")
);
CREATE INDEX "IDX_MARK_FEEDBACK" ON MARK("FEEDBACK_ID");

-- POST DEPLOY
insert into mark
  select
    id || '-mark' as id,
    id as feedback_id,
    uploaderid,
    sysdate as uploadeddate,
    'adjustment' as marktype,
    adjustedmark as mark,
    adjustedgrade as grade,
    adjustmentreason as reason,
    adjustmentcomments as comments
  from feedback where adjustedmark is not null;