alter table UpstreamAssessmentGroup
	add sequence NVARCHAR2(100);

-- drop the old index and make a new one
drop index idx_uag_ck;
create unique index idx_uag_ck on UpstreamAssessmentGroup (occurrence, modulecode, assessmentgroup, academicyear, sequence);