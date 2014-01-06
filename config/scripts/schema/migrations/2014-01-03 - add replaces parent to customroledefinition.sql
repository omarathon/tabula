--- TAB-1738

alter table CustomRoleDefinition add REPLACES_PARENT number(1,0) default 0;

create index IDX_CUSTOMRD_DEPTRP on CustomRoleDefinition (DEPARTMENT_ID, REPLACES_PARENT);