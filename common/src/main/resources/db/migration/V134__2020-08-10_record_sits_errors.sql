alter table recordedassessmentcomponentstudent
    add column last_sits_write_error_date timestamp(6),
    add column last_sits_write_error varchar;

alter table recordedmoduleregistration
    add column last_sits_write_error_date timestamp(6),
    add column last_sits_write_error varchar;
