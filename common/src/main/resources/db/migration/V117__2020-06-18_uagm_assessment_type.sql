alter table recordedassessmentcomponentstudent
    add column assessment_type varchar,
    add column resit_sequence varchar;
update recordedassessmentcomponentstudent
    set assessment_type = 'OriginalAssessment'
    where id in (
        select racs.id
        from recordedassessmentcomponentstudent racs
            join upstreamassessmentgroup uag on
                racs.module_code = uag.modulecode and
                racs.assessment_group = uag.assessmentgroup and
                racs.occurrence = uag.occurrence and
                racs.sequence = uag.sequence
            join upstreamassessmentgroupmember uagm on racs.university_id = uagm.universityid
        where not uagm.resitexpected
    );
-- These might end up being orphaned records because the UpstreamAssessmentGroup sequence might not match any more for resit by alternative assessment, nor the resit sequence
update recordedassessmentcomponentstudent
    set assessment_type = 'Reassessment', resit_sequence = '001'
    where id in (
        select racs.id
        from recordedassessmentcomponentstudent racs
            join upstreamassessmentgroup uag on
                racs.module_code = uag.modulecode and
                racs.assessment_group = uag.assessmentgroup and
                racs.occurrence = uag.occurrence and
                racs.sequence = uag.sequence
            join upstreamassessmentgroupmember uagm on racs.university_id = uagm.universityid
        where uagm.resitexpected
    );
-- This would only delete rows where students have been deregistered
delete from recordedassessmentcomponentstudentmark where recorded_assessment_component_student_id in (select id from recordedassessmentcomponentstudent where assessment_type is null);
delete from recordedassessmentcomponentstudent where assessment_type is null;
alter table recordedassessmentcomponentstudent alter column assessment_type set not null;

alter table upstreamassessmentgroupmember
    add column assessment_type varchar,
    add column resit_sequence varchar;
update upstreamassessmentgroupmember set assessment_type = 'OriginalAssessment' where assessment_type is null;
alter table upstreamassessmentgroupmember alter column assessment_type set not null;

insert into upstreamassessmentgroupmember (id, group_id, universityid, actualmark, actualgrade, agreedmark, agreedgrade, currentresitattempt, assessment_type, resit_sequence)
select id || '_resit', group_id, universityid, resitactualmark, resitactualgrade, resitagreedmark, resitagreedgrade, currentresitattempt, 'Reassessment', '001'
from upstreamassessmentgroupmember
where resitexpected;

