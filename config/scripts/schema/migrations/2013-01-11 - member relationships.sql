--- TAB-348
create table MemberRelationship (
	id NVARCHAR2(255) NOT NULL,
	subjectUniversityId NVARCHAR2(100) NOT NULL,
	relationshipType NVARCHAR2(50) NOT NULL,
	agent NVARCHAR2(255) NOT NULL,
	CONSTRAINT "MEMBERRELATIONSHIP_PK" PRIMARY KEY ("ID")
);

create index memberRelationship_subject on MemberRelationship(subjectUniversityId);
create index memberRelationship_subjectAndType on MemberRelationship(subjectUniversityId, relationshipType);