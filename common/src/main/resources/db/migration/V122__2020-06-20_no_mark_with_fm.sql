update recordedassessmentcomponentstudent
set needs_writing_to_sits = true
where id in (
    select racs.id
    from recordedassessmentcomponentstudent racs
        join recordedassessmentcomponentstudentmark racsm on
            racs.id = racsm.recorded_assessment_component_student_id and
            racsm.updated_date = (
                select max(racsm2.updated_date)
                from recordedassessmentcomponentstudentmark racsm2
                where racsm2.recorded_assessment_component_student_id = racsm.recorded_assessment_component_student_id
            )
    where racsm.grade = 'FM' and racsm.mark is not null
);

update recordedassessmentcomponentstudentmark
set mark = null
where id in (
    select racsm.id
    from recordedassessmentcomponentstudent racs
        join recordedassessmentcomponentstudentmark racsm on
            racs.id = racsm.recorded_assessment_component_student_id and
            racsm.updated_date = (
                select max(racsm2.updated_date)
                from recordedassessmentcomponentstudentmark racsm2
                where racsm2.recorded_assessment_component_student_id = racsm.recorded_assessment_component_student_id
            )
    where racsm.grade = 'FM' and racsm.mark is not null
);

update recordedmoduleregistration
set needs_writing_to_sits = true
where id in (
    select rmr.id
    from recordedmoduleregistration rmr
        join recordedmodulemark rmm on
            rmr.id = rmm.recorded_module_registration_id and
            rmm.updated_date = (
                select max(rmm2.updated_date)
                from recordedmodulemark rmm2
                where rmm2.recorded_module_registration_id = rmm.recorded_module_registration_id
            )
    where rmm.grade = 'FM' and rmm.mark is not null
);

update recordedmodulemark
set mark = null
where id in (
    select rmm.id
    from recordedmoduleregistration rmr
        join recordedmodulemark rmm on
            rmr.id = rmm.recorded_module_registration_id and
            rmm.updated_date = (
                select max(rmm2.updated_date)
                from recordedmodulemark rmm2
                where rmm2.recorded_module_registration_id = rmm.recorded_module_registration_id
            )
    where rmm.grade = 'FM' and rmm.mark is not null
);
