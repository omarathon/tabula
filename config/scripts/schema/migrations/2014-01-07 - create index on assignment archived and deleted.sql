--- TAB-1696

create index idx_assignment_arcdel on assignment (archived, deleted);
create unique index idx_uag_ck on UpstreamAssessmentGroup (occurrence, modulecode, assessmentgroup, academicyear);