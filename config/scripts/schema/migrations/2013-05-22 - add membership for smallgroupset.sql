create table SmallGroupSet_AssessmentGroup (
	smallgroupset_id nvarchar2(255) not null,
	assessmentgroup_id nvarchar2(255) not null,
	constraint SGS_AssGrp_PK primary key (smallgroupset_id, assessmentgroup_id)
);

create index idx_SGS_AssGrp_sgs_id on SmallGroupSet_AssessmentGroup(smallgroupset_id);
create index idx_SGS_AssGrp_AssGrp_id on SmallGroupSet_AssessmentGroup(assessmentgroup_id);

alter table SmallGroupSet add membersgroup_id nvarchar2(255);
alter table SmallGroup add studentsgroup_id nvarchar2(255);
alter table SmallGroupEvent add tutorsgroup_id nvarchar2(255);