-- MarkingWorkflowDao.getAssignmentsUsingMarkingWorkflow
create index idx_assignment_markscheme on assignment (markscheme_id);