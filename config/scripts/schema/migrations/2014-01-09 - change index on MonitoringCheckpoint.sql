-- TAB-1759 POST-DEPLOY

alter table monitoringcheckpoint
drop constraint POINT_SCD_UNIQUE;

alter table monitoringcheckpoint
add constraint point_student_unique unique (point_id, student_id);