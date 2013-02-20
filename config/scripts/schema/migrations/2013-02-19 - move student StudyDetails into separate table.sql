--- TAB-514

CREATE TABLE STUDYDETAILS (
	UNIVERSITYID NVARCHAR2(100) NOT NULL,
	HIB_VERSION NUMBER default 0,
	SPRCODE nvarchar2(100),
	SITSCOURSECODE nvarchar2(100),
	ROUTE_ID nvarchar2(255),
	STUDY_DEPARTMENT_ID nvarchar2(255),
	CONSTRAINT "STUDYDETAILS_PK" PRIMARY KEY ("UNIVERSITYID")
);

ALTER TABLE STUDYDETAILS ADD CONSTRAINT "STUDYDETAILS_FK" FOREIGN KEY (UNIVERSITYID) REFERENCES MEMBER(UNIVERSITYID);

insert into studydetails (universityid, sprcode, sitscoursecode, route_id, study_department_id)
(select universityid, sprcode, sitscoursecode, route_id, study_department_id from member where usertype = 'S');