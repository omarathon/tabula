--- TAB-348
create table StudentRelationship (
	id NVARCHAR2(255) NOT NULL,
	target_sprcode NVARCHAR2(100) NOT NULL,
	relationship_type NVARCHAR2(50) NOT NULL,
	agent NVARCHAR2(255) NOT NULL,
	uploaded_date timestamp,
	start_date timestamp,
	end_date timestamp,
	CONSTRAINT "MEMBERRELATIONSHIP_PK" PRIMARY KEY ("ID")
);

create index studentRelationship_target on StudentRelationship(target_SprCode);
create index studentRel_targetAndType on StudentRelationship(target_SprCode, relationship_Type);
