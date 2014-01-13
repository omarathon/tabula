--- TAB-1787

alter table Member add assistantsgroup_id nvarchar2(255);

create index idx_StaffMember_assistants on Member (assistantsgroup_id);