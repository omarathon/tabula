alter table extension drop constraint idx_extension_ck;

create index idx_extension_assignmentuserid on extension (assignment_id, userid);
create index idx_extension_assignmentuniversityid on extension (assignment_id, universityid);
