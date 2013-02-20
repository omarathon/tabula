--- TAB-19

create table CustomRoleDefinition (
	id NVARCHAR2(255) NOT NULL,
	department_id NVARCHAR2(255) NOT NULL,
	name NVARCHAR2(255) NOT NULL,
	custom_base_role_id NVARCHAR2(255),
	builtInBaseRoleDefinition NVARCHAR2(255),
	CONSTRAINT "CustomRoleDefinition_PK" PRIMARY KEY ("ID")
);

create index CustomRoleDefinition_customId on CustomRoleDefinition(custom_base_role_id);

create table RoleOverride (
	id NVARCHAR2(255) NOT NULL,
	permission NVARCHAR2(255) NOT NULL,
	overrideType NUMBER(1, 0) default 0,
	CONSTRAINT "RoleOverride_PK" PRIMARY KEY ("ID")
);

create table GrantedRole (
	id NVARCHAR2(255) NOT NULL,
	usergroup_id nvarchar2(255),
	custom_role_id NVARCHAR2(255),
	builtInRoleDefinition NVARCHAR2(255),
	scope_type NVARCHAR2(255) NOT NULL,
	scope_id NVARCHAR2(255) NOT NULL,
	CONSTRAINT "GrantedRole_PK" PRIMARY KEY ("ID")
);

CREATE INDEX IDX_GrantedRole_SCOPE on GrantedRole("SCOPE_TYPE", "SCOPE_ID");

create table GrantedPermission (
	id NVARCHAR2(255) NOT NULL,
	usergroup_id nvarchar2(255),
	permission NVARCHAR2(255) NOT NULL,
	overrideType NUMBER(1, 0) default 0,
	scope_type NVARCHAR2(255) NOT NULL,
	scope_id NVARCHAR2(255) NOT NULL,
	CONSTRAINT "GrantedPermission_PK" PRIMARY KEY ("ID")
);

CREATE INDEX IDX_GrantedPermission_SCOPE on grantedpermission("SCOPE_TYPE", "SCOPE_ID");