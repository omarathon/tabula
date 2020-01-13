-- to address additional sanity fails here - https://warwick.slack.com/archives/C0G854UCS/p1578595458017200

-- any cm1 migrated assignments should have single use marking workflows
update assignment set workflow_category = 'S' where cm1migration = true and cm2_workflow_id is not null;

-- 2 - workflows that were never completed should have outstanding stages set accordingly
delete from outstandingstages where feedback_id in (
    select f.id from markerfeedback mf
                         join feedback f on mf.feedback_id = f.id
                         join assignment a on f.assignment_id = a.id
                         join markingworkflow w on cm2_workflow_id = w.id
    where a.cm1migration = true and cm2_workflow_id is not null and mf.state = 'ReleasedForMarking'
    group by f.id
);

insert into outstandingstages (
    select f.id,
           case
               when w.workflowtype = 'Single' then 'single-marker'
               when w.workflowtype = 'Moderated' then 'moderation-marker'
               when w.workflowtype = 'Double' then 'dbl-first-marker'
               end as stage


    from markerfeedback mf
             join feedback f on mf.feedback_id = f.id
             join assignment a on f.assignment_id = a.id
             join markingworkflow w on cm2_workflow_id = w.id
    where a.cm1migration = true and cm2_workflow_id is not null and mf.state = 'ReleasedForMarking'
    group by f.id, w.workflowtype
);

delete from outstandingstages where feedback_id in (
    select f.id from markerfeedback mf
                         join feedback f on mf.feedback_id = f.id
                         join assignment a on f.assignment_id = a.id
                         join markingworkflow w on cm2_workflow_id = w.id
    where a.cm1migration = true and cm2_workflow_id is not null and mf.state = 'InProgress'
);

insert into outstandingstages (
    select f.id, mf.stage from markerfeedback mf
                                   join feedback f on mf.feedback_id = f.id
                                   join assignment a on f.assignment_id = a.id
                                   join markingworkflow w on cm2_workflow_id = w.id
    where a.cm1migration = true and cm2_workflow_id is not null and mf.state = 'InProgress'
);

-- 4 & 6 - set modified on for all cm1 migrated feedback
update markerfeedback set updated_on = uploaded_date where id in (
    select mf.id from markerfeedback mf
                          join feedback f on mf.feedback_id = f.id
                          join assignment a on f.assignment_id = a.id
    where a.cm1migration = true
);

-- 7 & 8 - delete extra marker feedback from redundant stages when the workflow was subsequently changed to single marker
delete from markerfeedback where stage in ('legacy-second-marker', 'legacy-third-marker');