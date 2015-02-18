-- TAB-3292
CREATE TABLE exam (
  id NVARCHAR2(100) NOT NULL,
  name nvarchar2(255),
  academicYear number(4,0) not null,
  module_id nvarchar2(255),
  membersgroup_id nvarchar2(255),
  deleted number(1,0) default 0 not null,
  CONSTRAINT "EXAM_PK" PRIMARY KEY ("ID")
);
CREATE INDEX "IDX_EXAM_MODULE" ON EXAM("MODULE_ID");

ALTER TABLE feedback ADD (
  DISCRIMINATOR NVARCHAR2(100) default 'assignment' not null,
  EXAM_ID NVARCHAR2(255) NULL
);
ALTER TABLE feedback MODIFY (
  ASSIGNMENT_ID NVARCHAR2(255) NULL
);

ALTER TABLE formfield ADD (
  EXAM_ID NVARCHAR2(255) NULL
);
ALTER TABLE formfield MODIFY (
  ASSIGNMENT_ID NVARCHAR2(255) NULL
);

ALTER TABLE AssessmentGroup ADD (
  EXAM_ID NVARCHAR2(255) NULL
);