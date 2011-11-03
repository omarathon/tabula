
    create table Assignment (
        id varchar2(255 char) not null,
        academicYear number(10,0) not null,
        active number(1,0) not null,
        attachmentLimit number(10,0) not null,
        closeDate timestamp,
        fileExtensions varchar2(255 char),
        name varchar2(255 char),
        openDate timestamp,
        module_id varchar2(255 char),
        CONSTRAINT "ASSIGNMENT_PK" PRIMARY KEY ("ID")
    );

    create table Department (
        id varchar2(255 char) not null,
        code varchar2(255 char),
        name varchar2(255 char),
        ownersgroup_id varchar2(255 char),
        CONSTRAINT "DEPARTMENT_PK" PRIMARY KEY ("ID")
    );
    
    CREATE INDEX IDX_MODULE_DEPT ON MODULE(DEPARTMENT_ID);

    create table FileAttachment (
        id varchar2(255 char) not null,
        data blob,
        name varchar2(255 char),
        submission_id varchar2(255 char),
       CONSTRAINT "FILEATTACHMENT_PK" PRIMARY KEY ("ID")
    );

    create table Module (
        id varchar2(255 char) not null,
        active number(1,0) not null,
        code varchar2(255 char),
        name varchar2(255 char),
        webgroup varchar2(255 char),
        department_id varchar2(255 char),
        CONSTRAINT "MODULE_PK" PRIMARY KEY ("ID"),
    	CONSTRAINT "MODULE_CODE" UNIQUE ("CODE")
    );

    create table Submission (
        id varchar2(255 char) not null,
        date timestamp,
        universityId varchar2(255 char) not null,
        userId varchar2(255 char) not null,
        assignment_id varchar2(255 char),
        primary key (id)
    );

    create table UserGroup (
        id varchar2(255 char) not null,
        baseWebgroup varchar2(255 char),
        CONSTRAINT "USERGROUP_PK" PRIMARY KEY ("ID")
    );

    create table UserGroupExclude (
        group_id varchar2(255 char) not null,
        usercode varchar2(255 char)
    );

    create table UserGroupInclude (
        group_id varchar2(255 char) not null,
        usercode varchar2(255 char)
    );

    CREATE INDEX IDX_USERGROUPINC ON USERGROUPINCLUDE(GROUP_ID);
	CREATE INDEX IDX_USERGROUPINC ON USERGROUPEXCLUDE(GROUP_ID);
