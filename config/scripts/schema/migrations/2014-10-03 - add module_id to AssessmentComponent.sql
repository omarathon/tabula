-- TAB-2810
alter table UpstreamAssignment add module_id nvarchar2(255);

create index idx_upstreamassignment_modid on UpstreamAssignment(module_id);

