--- TAB-348
create table MemberRelationship (
	id NVARCHAR2(255) NOT NULL,
	targetUniversityId NVARCHAR2(100) NOT NULL,
	relationshipType NVARCHAR2(50) NOT NULL,
	agent NVARCHAR2(255) NOT NULL,
	CONSTRAINT "MEMBERRELATIONSHIP_PK" PRIMARY KEY ("ID")
);

create index memberRelationship_target on MemberRelationship(targetUniversityId);
create index memberRel_targetAndType on MemberRelationship(targetUniversityId, relationshipType);