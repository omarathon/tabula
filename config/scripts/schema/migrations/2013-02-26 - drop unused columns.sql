-- TAB-461

alter table DEPARTMENT set unused column COLLECTFEEDBACKRATINGS;
alter table DEPARTMENT set unused column ALLOWEXTENSIONREQUESTS;
alter table DEPARTMENT set unused column EXTENSIONGUIDELINESUMMARY;
alter table DEPARTMENT set unused column EXTENSIONGUIDELINELINK;
alter table DEPARTMENT set unused column SHOWSTUDENTNAME;
alter table DEPARTMENT set unused column OWNERSGROUP_ID;
alter table DEPARTMENT set unused column EXTENSION_MANAGERS_ID;
alter table DEPARTMENT drop unused columns;

alter table MODULE set unused column PARTICIPANTSGROUP_ID;
alter table MODULE drop unused columns;

alter table ROUTE set unused column PARTICIPANTSGROUP_ID;
alter table ROUTE drop unused columns;

alter table member set unused column STUDY_DEPARTMENT_ID;
alter table member set unused column SPRCODE;
alter table member set unused column SITSCOURSECODE;
alter table member set unused column ROUTE_ID;
alter table member set unused column YEAROFSTUDY;
alter table member set unused column ATTENDANCEMODE;
alter table member set unused column STUDENTSTATUS;
alter table member set unused column FUNDINGSOURCE;
alter table member set unused column PROGRAMMEOFSTUDY;
alter table member set unused column INTENDEDAWARD;
alter table member set unused column ACADEMICYEAR;
alter table member set unused column COURSESTARTYEAR;
alter table member set unused column YEARCOMMENCEDDEGREE;
alter table member set unused column COURSEBASEYEAR;
alter table member set unused column COURSEENDDATE;
alter table member set unused column TRANSFERREASON;
alter table member set unused column BEGINDATE;
alter table member set unused column ENDDATE;
alter table member set unused column EXPECTEDENDDATE;
alter table member set unused column FEESTATUS;
alter table member set unused column DOMICILE;
alter table member set unused column HIGHESTQUALIFICATIONONENTRY;
alter table member set unused column LASTINSTITUTE;
alter table member set unused column LASTSCHOOL;
alter table member drop unused columns;