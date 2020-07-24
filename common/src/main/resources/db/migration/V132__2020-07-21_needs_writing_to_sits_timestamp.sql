alter table recordedassessmentcomponentstudent add column needs_writing_to_sits_since timestamp(6);
update recordedassessmentcomponentstudent racs
set needs_writing_to_sits_since = (select max(racsm.updated_date) from recordedassessmentcomponentstudentmark racsm where racsm.recorded_assessment_component_student_id = racs.id)
where needs_writing_to_sits;

alter table recordedmoduleregistration add column needs_writing_to_sits_since timestamp(6);
update recordedmoduleregistration rmr
set needs_writing_to_sits_since = (select max(rmm.updated_date) from recordedmodulemark rmm where rmm.recorded_module_registration_id = rmr.id)
where needs_writing_to_sits;

alter table recordedresit add column needs_writing_to_sits_since timestamp(6);
update recordedresit set needs_writing_to_sits_since = current_timestamp where needs_writing_to_sits;
