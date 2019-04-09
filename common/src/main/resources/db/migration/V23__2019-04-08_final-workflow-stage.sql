alter table feedback add column finalstage varchar(255);

-- Marker chooses - populate final stage
update feedback f set finalstage = 'moderation-marker' where id in (
  select f.id
  from feedback f
    join assignment a on a.id = f.assignment_id
    join markingworkflow m on a.cm2_workflow_id = m.id
    join auditevent ae on ae.eventtype = 'FinishMarking' and ae.eventstage = 'before' and ae.data::jsonb -> 'assignment' ? a.id and ae.data::jsonb -> 'students' ? f.userid
  where m.settings::jsonb -> 'moderationSampler' ? 'marker'
);

update feedback f set finalstage = 'moderation-moderator' where id in (
  select f.id from feedback f -- there wouldn't be any MarkingCompleted events if this hadn't gone via the moderator
    join assignment a on a.id = f.assignment_id
    join markingworkflow m on a.cm2_workflow_id = m.id
    join auditevent ae on ae.eventtype = 'MarkingCompleted' and ae.eventstage = 'before' and ae.data::jsonb -> 'assignment' ? a.id and ae.data::jsonb -> 'students' ? f.userid
  where m.settings::jsonb -> 'moderationSampler' ? 'marker'
);

-- Admin chooses - populate final stage
update feedback f set finalstage = 'moderation-marker' where id in (
  select f.id
  from feedback f
    join assignment a on a.id = f.assignment_id
    join markingworkflow m on a.cm2_workflow_id = m.id
    join auditevent ae on ae.eventtype = 'AdminFinalise' and ae.eventstage = 'before' and ae.data::jsonb -> 'assignment' ? a.id and  ae.data::jsonb -> 'studentUsercodes' ? f.userid
  where m.settings::jsonb -> 'moderationSampler' ? 'admin'
);

update feedback f set finalstage = 'moderation-moderator' where id in (
  select f.id from feedback f
    join assignment a on a.id = f.assignment_id
    join markingworkflow m on a.cm2_workflow_id = m.id
    join auditevent ae on ae.eventtype = 'AllocateModerators' and ae.eventstage = 'before' and ae.data::jsonb -> 'assignment' ? a.id and ae.data::jsonb -> 'studentUsercodes' ? f.userid
  where m.settings::jsonb -> 'moderationSampler' ? 'admin'
);

-- Moderator chooses - populate all final stages - we have to assume that every submission was moderated as pre TAB-7031 no separate skip moderation action existed
update feedback f set finalstage = 'moderation-moderator' where id in (
  select f.id from feedback f
    join assignment a on a.id = f.assignment_id
    join markingworkflow m on a.cm2_workflow_id = m.id
  where m.settings::jsonb -> 'moderationSampler' ? 'moderator'
);