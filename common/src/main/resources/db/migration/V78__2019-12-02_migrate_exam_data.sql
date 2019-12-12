-- marking workflows
insert into markingworkflow (id, workflowtype, name, department_id, is_reusable, academicyear)
select concat(e.id,'-workflow') as id, 'Single' as workflowtype, ms.name, ms.department_id, false as is_reusable, e.academicyear
from MarkScheme ms join exam e on e.workflow_id = ms.id;

-- markers on workflows
insert into usergroup (id, universityids)
select concat(e.id,'-markers-ug') as id, false as universityids from MarkScheme ms join exam e on e.workflow_id = ms.id;

insert into usergroupinclude (group_id, usercode)
select concat(e.id,'-markers-ug'), ug.usercode from MarkScheme ms join exam e on e.workflow_id = ms.id join usergroupinclude ug on ms.firstmarkers_id = ug.group_id;

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(e.id,'-stagemarkers') as id, 'single-marker' as stage, concat(e.id,'-markers-ug'), concat(e.id,'-workflow') as workflow_id
from MarkScheme ms join exam e on e.workflow_id = ms.id;


-- assignments from exams
insert into assignment (id, name, academicyear, module_id, membersgroup_id, deleted, archived, collectMarks, collectSubmissions, settings, cm2_workflow_id, workflow_category, cm2assignment, attachmentlimit, createddate, opendate, openended)
select id, name, academicyear, module_id, membersgroup_id, deleted,
       false as archived,
       true as collectMarks,
       false as collectSubmissions,
       '{"useMarkPoints":false,"publishFeedback":false,"includeInFeedbackReportWithoutSubmissions":false,"automaticallyReleaseToMarkers":false}' as settings,
       case when e.workflow_id is not null then concat(id,'-workflow') end as cm2_workflow_id,
       'S' as workflow_category,
       true as cm2assignment,
       1 as attachmentlimit,
       createddate,
       createddate as opendate,
       true as openended
from exam e join (select exam_id, min(uploaded_date) as createddate from feedback group by exam_id) date on date.exam_id = e.id;

-- copy feedback to the new assignments
select * into temp table exam_feedback from feedback where exam_id is not null;
update exam_feedback set id = concat(id, '-copy'), assignment_id = exam_id, discriminator = 'assignment';
update exam_feedback set exam_id = null;
insert into feedback select * from exam_feedback;
drop table exam_feedback;

-- copy feedbackforsits to the new assignments
select ffs.* into temp table exam_feedbackforsits from feedbackforsits ffs join feedback f on ffs.feedback_id = f.id where f.exam_id is not null;
update exam_feedbackforsits set id = concat(id, '-copy'), feedback_id = concat(feedback_id, '-copy');
insert into feedbackforsits select * from exam_feedbackforsits;
drop table exam_feedbackforsits;

-- copy adjustments to the new assignments
select m.* into temp table exam_marks from mark m join feedback f on m.feedback_id = f.id where f.exam_id is not null;
update exam_marks set id = concat(id, '-copy'), feedback_id = concat(feedback_id, '-copy');
insert into mark select * from exam_marks;
drop table exam_marks;

-- marker allocation
insert into markerfeedback (id, mark, uploaded_date, feedback_id, grade, marker, stage, updated_on)
select concat(f.id,'-markerfeedback') as id, f.actualmark as mark, f.uploaded_date, concat(f.id, '-copy'), f.actualgrade, mug.marker_uni_id as marker, 'single-marker' as stage, f.updated_date as updated_on from feedback f
    join marker_usergroup mug on mug.exam_id = f.exam_id and mug.discriminator = 'first'
    join usergroupinclude u on mug.markermap_id = u.group_id and u.usercode = f.userid
where f.exam_id is not null;

insert into outstandingstages
select concat(id, '-copy') as feedback_id, 'single-marking-completed' as stage from feedback where exam_id is not null;

-- copy linked SITS assessment components
select * into temp table exam_assessmentgroup from assessmentgroup where exam_id is not null;
update exam_assessmentgroup set id = concat(id, '-copy'), assignment_id = exam_id;
update exam_assessmentgroup set exam_id = null;
insert into assessmentgroup select * from exam_assessmentgroup;
drop table exam_assessmentgroup;



