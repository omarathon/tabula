
create table Assignment (
    id nvarchar2(255) not null,
    academicYear number(10,0) not null,
    active number(1,0) not null,
    attachmentLimit number(10,0) not null,
    closeDate timestamp,
    fileExtensions nvarchar2(255),
    name nvarchar2(255),
    openDate timestamp,
    module_id nvarchar2(255),
    CONSTRAINT "ASSIGNMENT_PK" PRIMARY KEY ("ID")
);

create table Department (
    id nvarchar2(255) not null,
    code nvarchar2(255),
    name nvarchar2(255),
    ownersgroup_id nvarchar2(255),
    CONSTRAINT "DEPARTMENT_PK" PRIMARY KEY ("ID")
);

CREATE INDEX IDX_MODULE_DEPT ON MODULE(DEPARTMENT_ID);

create table FileAttachment (
    id nvarchar2(255) not null,
    data blob,
    name nvarchar2(255),
    submission_id nvarchar2(255),
   CONSTRAINT "FILEATTACHMENT_PK" PRIMARY KEY ("ID")
);

create table Module (
    id nvarchar2(255) not null,
    active number(1,0) not null,
    code nvarchar2(255),
    name nvarchar2(255),
    webgroup nvarchar2(255),
    department_id nvarchar2(255),
    CONSTRAINT "MODULE_PK" PRIMARY KEY ("ID"),
	CONSTRAINT "MODULE_CODE" UNIQUE ("CODE")
);

create table Submission (
    id nvarchar2(255) not null,
    date timestamp,
    universityId nvarchar2(255) not null,
    userId nvarchar2(255) not null,
    assignment_id nvarchar2(255),
    primary key (id)
);

create table UserGroup (
    id nvarchar2(255) not null,
    baseWebgroup nvarchar2(255),
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

CREATE INDEX IDX_USERGROUPINC ON USERGROUPINCLUDE(GROUP_ID);
CREATE INDEX IDX_USERGROUPINC ON USERGROUPEXCLUDE(GROUP_ID);

create table AuditEvent (
	eventdate timestamp,
	eventType nvarchar2(255) not null,
	eventStage nvarchar2(64) not null,
	real_user_id nvarchar2(255) not null,
	masquerade_user_id nvarchar2(255) not null,
	data nvarchar2(4000) not null
);

create index idx_auditeventdate on auditevent(eventdate);
