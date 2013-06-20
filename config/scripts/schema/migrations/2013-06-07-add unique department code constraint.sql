-- TAB-579 department code needs to be unique so we can use it as an fk in studentCourseDetails
alter table department add constraint dept_code UNIQUE (code);

