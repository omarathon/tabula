-- TAB-134 Optionally prefix submission witih the student name

ALTER TABLE Department
ADD (
	showStudentName number(1,0) DEFAULT 0 NOT NULL
);
