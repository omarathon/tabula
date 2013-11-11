--- TAB-1510
alter table studentcourseyeardetails add enrolment_department_id nvarchar2(255);
create index IDX_SCYD_DEPT on studentcourseyeardetails(enrolment_department_id);