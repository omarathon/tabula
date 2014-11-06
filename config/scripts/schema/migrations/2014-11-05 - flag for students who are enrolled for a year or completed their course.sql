-- TAB-2902
alter table StudentCourseDetails add reasonForTransferCode NVARCHAR2(10);

alter table StudentCourseYearDetails add enrolledOrCompleted NUMBER(1, 0);
update StudentCourseYearDetails set enrolledOrCompleted = 1 where enrolmentStatusCode not like 'P%';
update StudentCourseYearDetails set enrolledOrCompleted = 0 where enrolledOrCompleted is null;
alter table StudentCourseYearDetails modify enrolledOrCompleted default 0;