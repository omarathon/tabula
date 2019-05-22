alter table feedback drop constraint idx_feedback_ck;

create unique index idx_feedback_assignment_universityid on feedback (assignment_id, universityid)
  where assignment_id is not null;

create unique index idx_feedback_exam_universityid on feedback (exam_id, universityid)
  where exam_id is not null;

alter table grantedrole drop constraint idx_grantedrole_type;

create unique index idx_grantedrole_builtinroledefinition on grantedrole (scope_type, scope_id, builtinroledefinition)
  where builtinroledefinition is not null;

create unique index idx_grantedrole_customrole on grantedrole (scope_type, scope_id, custom_role_id)
  where custom_role_id is not null;
