--- TAB-3420
drop index idx_feedback_ck;
create unique index idx_feedback_ck on feedback (universityid, assignment_id, exam_id);