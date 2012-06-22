-- canonical script for generating the database schema from scratch.
-- when you create a migration script, you should also update this
-- to produce the same results.

create table Assignment (
    id nvarchar2(255) not null,
    academicYear number(4,0) not null,
    active number(1,0) not null,
    attachmentLimit number(10,0) not null,
    closeDate timestamp,
    fileExtensions nvarchar2(255),
    name nvarchar2(255),
    openDate timestamp,
    module_id nvarchar2(255),
    collectMarks number(1,0) not null,
    displayPlagiarismNotice number(1,0) default 0 not null,
    CONSTRAINT "ASSIGNMENT_PK" PRIMARY KEY ("ID")
);
CREATE INDEX "IDX_ASSIGNMENT_MODULE" ON ASSIGNMENT("MODULE_ID");

create table Department (
    id nvarchar2(255) not null,
    code nvarchar2(255),
    name nvarchar2(255),
    ownersgroup_id nvarchar2(255),
    collectFeedbackRatings NUMBER(1,0) DEFAULT 1 NOT NULL,
    CONSTRAINT "DEPARTMENT_PK" PRIMARY KEY ("ID")
);


create table FileAttachment (
    id nvarchar2(255) not null,
    name nvarchar2(255),
    temporary number(1,0) not null,
    feedback_id nvarchar2(255),
    submission_id nvarchar2(255),
    dateUploaded timestamp not null,
    CONSTRAINT "FILEATTACHMENT_PK" PRIMARY KEY ("ID")
);
create index fileattachment_feedback on fileattachment(feedback_id);
create index fileattachment_submission on fileattachment(submission_id);
create index fileattachment_temporary on fileattachment(temporary, dateUploaded);

create table Module (
    id nvarchar2(255) not null,
    active number(1,0) not null,
    code nvarchar2(255),
    name nvarchar2(255),
    membersgroup_id nvarchar2(255),
    participantsgroup_id nvarchar2(255),
    department_id nvarchar2(255),
    CONSTRAINT "MODULE_PK" PRIMARY KEY ("ID"),
	CONSTRAINT "MODULE_CODE" UNIQUE ("CODE")
);
CREATE INDEX IDX_MODULE_DEPT ON MODULE(DEPARTMENT_ID);

create table Feedback (
	id nvarchar2(255) not null,
	uploaderid nvarchar2(255) not null,
	uploaded_date timestamp not null,
	universityId nvarchar2(255) not null,
	assignment_id nvarchar2(255) not null,
	actualMark NUMBER(5, 0),
	actualGrade nvarchar2(255),
	agreedMark NUMBER(5, 0),
	agreedGrade nvarchar2(255),
	RELEASED NUMBER(1, 0) default 0 not null,
	ratingPrompt NUMBER(1,0),
	ratingHelpful NUMBER(1,0),
	CONSTRAINT "FEEDBACK_PK" PRIMARY KEY ("ID")
);
CREATE INDEX IDX_FEEDBACK_ASSIGNMENT ON FEEDBACK(ASSIGNMENT_ID);

create table Submission (
    id nvarchar2(255) not null,
    submitted number(1,0) not null,
    submitted_date timestamp,
    universityId nvarchar2(255) not null,
    userId nvarchar2(255) not null,
    assignment_id nvarchar2(255),
    constraint "SUBMISSION_PK" PRIMARY KEY ("ID")
);
create index idx_submission_assignment on SUBMISSION("ASSIGNMENT_ID");
create index idx_submission_user on SUBMISSION("USERID");

create table UserGroup (
    id nvarchar2(255) not null,
    baseWebgroup nvarchar2(255),
    UNIVERSITYIDS NUMBER(1,0) DEFAULT 0,
    CONSTRAINT "USERGROUP_PK" PRIMARY KEY ("ID")
);
create table UserGroupExclude (
    group_id nvarchar2(255) not null,
    usercode nvarchar2(255)
);
create table UserGroupInclude (
    group_id nvarchar2(255) not null,
    usercode nvarchar2(255)
);
CREATE TABLE USERGROUPSTATIC (
    group_id nvarchar2(255) not null,
    usercode nvarchar2(255)
);
CREATE INDEX IDX_USERGROUPINC ON USERGROUPINCLUDE(GROUP_ID);
CREATE INDEX IDX_USERGROUPEXC ON USERGROUPEXCLUDE(GROUP_ID);
CREATE INDEX IDX_USERGROUPSTATIC ON USERGROUPSTATIC(GROUP_ID);

create table AuditEvent (
	ID NUMBER(38, 0),
	EVENTID NCHAR(36),
	eventdate timestamp,
	eventType nvarchar2(255) not null,
	eventStage nvarchar2(64) not null,
	real_user_id nvarchar2(255),
	masquerade_user_id nvarchar2(255),
	data nvarchar2(4000) not null
	CONSTRAINT "auditevent_pk" PRIMARY KEY ("ID")
);
create index idx_auditeventdate on auditevent(eventdate);
CREATE INDEX "IDX_AUDITEVENT_EVENTID" ON AUDITEVENT(EVENTID);
CREATE SEQUENCE "AUDITEVENT_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

create table formfield (
    id nvarchar2(255) not null,
    assignment_id nvarchar2(255) not null,
    name nvarchar2(255) not null,
    position number(3,0) not null,
    label nvarchar2(255),
    instructions nvarchar2(4000),
    fieldtype nvarchar2(255) not null,
    required number(1,0) not null,
    properties nvarchar2(4000) not null,
    CONSTRAINT "formfield_pk" PRIMARY KEY ("ID")
);
create index idx_formfieldassignment on formfield(assignment_id);

CREATE TABLE SUBMISSIONVALUE (
	id nvarchar2(100) not null,
	name nvarchar2(255) not null,
	submission_id nvarchar2(255) not null,
	VALUE nvarchar2(4000),
	CONSTRAINT "SUBMISSIONVALUE_PK" PRIMARY KEY ("ID")
);
CREATE INDEX "IDX_SUBMISSIONVALUE_SUBMISSION" ON SUBMISSIONVALUE(SUBMISSION_ID);

-- HFC-33

CREATE TABLE UPSTREAMASSIGNMENT (
	ID NVARCHAR2(100) NOT NULL,
	MODULECODE NVARCHAR2(100) NOT NULL,
	ASSESSMENTGROUP NVARCHAR2(100) NOT NULL,
	SEQUENCE NVARCHAR2(100) NOT NULL,
	DEPARTMENTCODE NVARCHAR2(10) NOT NULL,
	NAME NVARCHAR2(4000) NOT NULL,
	CONSTRAINT "UPSTREAMASSIGNMENT_PK" PRIMARY KEY ("ID")
);
CREATE UNIQUE INDEX IDX_UPSTREAMASSIGNMENT_MAIN ON UPSTREAMASSIGNMENT(MODULECODE, SEQUENCE);
CREATE INDEX IDX_UPSTREAMASSIGNMENT_DEPT ON UPSTREAMASSIGNMENT(DEPARTMENTCODE);

CREATE TABLE UPSTREAMASSESSMENTGROUP (
	ID NVARCHAR2(100) NOT NULL,
	MODULECODE NVARCHAR2(100) NOT NULL,
	ASSESSMENTGROUP NVARCHAR2(100) NOT NULL,
	ACADEMICYEAR NUMBER(4,0) NOT NULL,
	OCCURRENCE NVARCHAR2(100) NOT NULL,
	MEMBERSGROUP_ID NVARCHAR2(4000),
	CONSTRAINT "UPSTREAMASSESSMENTGROUP_PK" PRIMARY KEY ("ID")
);
CREATE INDEX IDX_UAG_YEAR ON UPSTREAMASSESSMENTGROUP(ACADEMICYEAR);

CREATE TABLE JOB (
	ID NVARCHAR2(100) NOT NULL,
	
	JOBTYPE NVARCHAR2(100) NOT NULL,
	STATUS NVARCHAR2(4000),
	DATA CLOB NOT NULL,
	STARTED NUMBER(1,0) DEFAULT 0 NOT NULL,
	FINISHED NUMBER(1,0) DEFAULT 0 NOT NULL,
	SUCCEEDED NUMBER(1,0) DEFAULT 0 NOT NULL,
	PROGRESS NUMBER(3,0),
	
	CREATEDDATE TIMESTAMP NOT NULL,
	UPDATEDDATE TIMESTAMP NOT NULL,
	
	CONSTRAINT "JOB_PK" PRIMARY KEY ("ID")
);
CREATE INDEX "IDX_JOBSTARTED" ON JOB("STARTED","FINISHED");
