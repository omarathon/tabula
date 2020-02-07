-- Reverts V89
drop index if exists idx_submissionvalue_markerfeedback;
create index idx_submissionvalue_markerfeedback on submissionvalue (marker_feedback_id, name);
