alter table studydetails add (
	intendedaward nvarchar2(12),
	begindate date,
	enddate date,
	expectedenddate date,
	fundingsource nvarchar2(6),
	courseyearlength number(2),
	sprstatuscode nvarchar2(6);
	enrolmentstatuscode nvarchar2(6)
);
