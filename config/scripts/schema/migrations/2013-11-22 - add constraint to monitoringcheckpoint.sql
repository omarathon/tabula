alter table monitoringcheckpoint
add constraint point_scd_unique unique (point_id, student_course_detail_id);