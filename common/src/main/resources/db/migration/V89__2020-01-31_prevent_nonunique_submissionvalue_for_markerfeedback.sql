-- Prevent TAB-8090 breaking things (will probably just push the exception up the stack to where the bad happens)
create unique index idx_submissionvalue_markerfeedback on submissionvalue (marker_feedback_id, name);
