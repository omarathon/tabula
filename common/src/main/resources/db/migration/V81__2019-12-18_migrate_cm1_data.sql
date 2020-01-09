alter table assignment add column cm1Migration boolean default false;

-- marking workflows
insert into markingworkflow (id, workflowtype, name, department_id, is_reusable, academicyear)
select ms.id || '-cm1migration',
    case
        when ms.markingmethod = 'FirstMarkerOnly' then 'Single'
        when ms.markingmethod = 'ModeratedMarking' then 'Moderated'
        when ms.markingmethod = 'StudentsChooseMarker' then 'Single'
        when ms.markingmethod = 'SeenSecondMarking' then 'Double'
        when ms.markingmethod = 'SeenSecondMarkingNew' then 'Double'
        end as workflowtype,
    ms.name, ms.department_id, true as is_reusable, '2012' as academicyear
from MarkScheme ms;

-- marker stages
insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-single') as id, 'single-marker' as stage, ms.firstmarkers_id, ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'FirstMarkerOnly' or ms.markingmethod = 'StudentsChooseMarker';

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-moderation-marker') as id, 'moderation-marker' as stage, ms.firstmarkers_id, ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'ModeratedMarking';

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-moderation-moderator') as id, 'moderation-moderator' as stage, ms.secondmarkers_id, ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'ModeratedMarking';

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-dbl-first-marker') as id, 'dbl-first-marker' as stage, ms.firstmarkers_id, ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'SeenSecondMarking' or ms.markingmethod = 'SeenSecondMarkingNew';

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-dbl-second-marker') as id, 'dbl-second-marker' as stage, ms.secondmarkers_id, ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'SeenSecondMarking' or ms.markingmethod = 'SeenSecondMarkingNew';

-- need to copy usergroups for final markers because of idx_stagemarkers_group
insert into usergroup (id, universityids)
select concat(u.id,'-final-markers-ug') as id, false as universityids
from MarkScheme ms join usergroup u on ms.firstmarkers_id = u.id left join exam e on ms.id = e.workflow_id
where e.id is null and ms.markingmethod = 'SeenSecondMarking' or ms.markingmethod = 'SeenSecondMarkingNew';

insert into usergroupinclude (group_id, usercode)
select concat(ug.group_id,'-final-markers-ug'), ug.usercode
from MarkScheme ms join usergroupinclude ug on ms.firstmarkers_id = ug.group_id left join exam e on ms.id = e.workflow_id
where e.id is null and ms.markingmethod = 'SeenSecondMarking' or ms.markingmethod = 'SeenSecondMarkingNew';

insert into stagemarkers (id, stage, markers, workflow_id)
select concat(ms.id,'-dbl-final-marker') as id, 'dbl-final-marker' as stage, concat(ms.firstmarkers_id,'-final-markers-ug'), ms.id || '-cm1migration' as workflow_id
from MarkScheme ms where ms.markingmethod = 'SeenSecondMarking' or ms.markingmethod = 'SeenSecondMarkingNew';

-- marker allocation
update markerfeedback mf set marker = subquery.marker
from (
    select mug.marker_uni_id as marker, mf.id as id from markerfeedback mf
        join feedback f on mf.feedback_id = f.id
        join assignment a on f.assignment_id = a.id
        join marker_usergroup mug on mug.assignment_id = f.assignment_id and mug.discriminator = 'first'
        join usergroupinclude u on mug.markermap_id = u.group_id and u.usercode = f.userid
    where a.cm2assignment is false and mf.stage in ('legacy-first-marker', 'legacy-third-marker')
) as subquery
where subquery.id = mf.id;

update markerfeedback mf set marker = subquery.marker
from (
    select mug.marker_uni_id as marker, mf.id as id from markerfeedback mf
        join feedback f on mf.feedback_id = f.id
        join assignment a on f.assignment_id = a.id
        join marker_usergroup mug on mug.assignment_id = f.assignment_id and mug.discriminator = 'second'
        join usergroupinclude u on mug.markermap_id = u.group_id and u.usercode = f.userid
    where a.cm2assignment is false and a.deleted is false and mf.stage = 'legacy-second-marker'
) as subquery
where subquery.id = mf.id;

update markerfeedback mf set stage = subquery.stage
from (select mf.id,
    case
        when ms.markingmethod = 'FirstMarkerOnly' then 'single-markersingle-marker'
        when ms.markingmethod = 'ModeratedMarking' then 'moderation-marker'
        when ms.markingmethod = 'StudentsChooseMarker' then 'single-markersingle-marker'
        when ms.markingmethod = 'SeenSecondMarking' then 'dbl-first-marker'
        when ms.markingmethod = 'SeenSecondMarkingNew' then 'dbl-first-marker'
        end as stage
    from markerfeedback mf
        join feedback f on mf.feedback_id = f.id
        join assignment a on a.id = f.assignment_id
        join MarkScheme ms on ms.id = a.markscheme_id
    where a.cm2assignment is false and a.deleted is false and stage = 'legacy-first-marker') as subquery
where subquery.id = mf.id;

update markerfeedback mf set stage = subquery.stage
from (select mf.id,
    case
        when ms.markingmethod = 'FirstMarkerOnly' then 'legacy-second-marker'
        when ms.markingmethod = 'ModeratedMarking' then 'moderation-moderator'
        when ms.markingmethod = 'StudentsChooseMarker' then 'legacy-second-marker'
        when ms.markingmethod = 'SeenSecondMarking' then 'dbl-second-marker'
        when ms.markingmethod = 'SeenSecondMarkingNew' then 'dbl-second-marker'
        end as stage
    from markerfeedback mf
        join feedback f on mf.feedback_id = f.id
        join assignment a on a.id = f.assignment_id
        join MarkScheme ms on ms.id = a.markscheme_id
    where a.cm2assignment is false and a.deleted is false and stage = 'legacy-second-marker') as subquery
where subquery.id = mf.id;

update markerfeedback mf set stage = subquery.stage
from (select mf.id,
    case
        when ms.markingmethod = 'FirstMarkerOnly' then 'legacy-third-marker'
        when ms.markingmethod = 'ModeratedMarking' then 'legacy-third-marker'
        when ms.markingmethod = 'StudentsChooseMarker' then 'legacy-third-marker'
        when ms.markingmethod = 'SeenSecondMarking' then 'dbl-final-marker'
        when ms.markingmethod = 'SeenSecondMarkingNew' then 'dbl-final-marker'
        end as stage
    from markerfeedback mf
        join feedback f on mf.feedback_id = f.id
        join assignment a on a.id = f.assignment_id
        join MarkScheme ms on ms.id = a.markscheme_id
    where a.cm2assignment is false and a.deleted is false and stage = 'legacy-third-marker') as subquery
where subquery.id = mf.id;

insert into outstandingstages
select f.id as feedback_id,
    case
        when ms.markingmethod = 'FirstMarkerOnly' then 'single-marking-completed'
        when ms.markingmethod = 'ModeratedMarking' then 'moderation-completed'
        when ms.markingmethod = 'StudentsChooseMarker' then 'single-marking-completed'
        when ms.markingmethod = 'SeenSecondMarking' then 'dbl-Completed'
        when ms.markingmethod = 'SeenSecondMarkingNew' then 'dbl-Completed'
        end as stage
from feedback f join assignment a on f.assignment_id = a.id join MarkScheme ms on ms.id = a.markscheme_id where a.cm2assignment is false and a.deleted is false;

update assignment set cm2assignment = true, cm2_workflow_id = markscheme_id || '-cm1migration', markscheme_id = null, cm1Migration = true where deleted = false and cm2assignment is false;