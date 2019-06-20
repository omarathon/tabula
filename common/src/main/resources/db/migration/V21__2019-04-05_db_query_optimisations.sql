-- Allow getting Assignments and SmallGroupSets by SITS enrolment to avoid a seq scan on UAGM
create index idx_upstreamassessmentgroupmember_universityid on upstreamassessmentgroupmember (universityid);

-- Try and help Postgres optimise getting Assignments and SGS by SITS enrolment
create index idx_assignment_arcdelhidden on assignment (archived, deleted, hidden_from_students);
create index idx_smallgroupset_arcdel on smallgroupset (archived, deleted);
