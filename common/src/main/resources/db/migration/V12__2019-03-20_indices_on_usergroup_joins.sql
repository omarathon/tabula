create unique index idx_assignment_membersgroup on assignment (membersgroup_id);
alter table assignment add constraint fk_assignment_membersgroup foreign key (membersgroup_id) references usergroup (id);

create unique index idx_exam_membersgroup on exam (membersgroup_id);
alter table exam add constraint fk_exam_membersgroup foreign key (membersgroup_id) references usergroup (id);

create unique index idx_markscheme_firstmarkers on markscheme (firstmarkers_id);
create unique index idx_markscheme_secondmarkers on markscheme (secondmarkers_id);
alter table markscheme add constraint fk_markscheme_firstmarkers foreign key (firstmarkers_id) references usergroup (id);
alter table markscheme add constraint fk_markscheme_secondmarkers foreign key (secondmarkers_id) references usergroup (id);

create unique index idx_stagemarkers_group on stagemarkers (markers);
alter table stagemarkers add constraint fk_stagemarkers_group foreign key (markers) references usergroup (id);

create unique index idx_member_assistants on member (assistantsgroup_id);
alter table member add constraint fk_member_assistants foreign key (assistantsgroup_id) references usergroup (id);

create unique index idx_smallgroupevent_tutorsgroup on smallgroupevent (tutorsgroup_id);
alter table smallgroupevent add constraint fk_smallgroupevent_tutorsgroup foreign key (tutorsgroup_id) references usergroup (id);
create unique index idx_smallgroup_studentsgroup on smallgroup (studentsgroup_id);
alter table smallgroup add constraint fk_smallgroup_studentsgroup foreign key (studentsgroup_id) references usergroup (id);
create unique index idx_smallgroupset_membersgroup on smallgroupset (membersgroup_id);
alter table smallgroupset add constraint fk_smallgroupset_membersgroup foreign key (membersgroup_id) references usergroup (id);

create unique index idx_departmentsmallgroup_studentsgroup on departmentsmallgroup (studentsgroup_id);
alter table departmentsmallgroup add constraint fk_departmentsmallgroup_studentsgroup foreign key (studentsgroup_id) references usergroup (id);
create unique index idx_departmentsmallgroupset_membersgroup on departmentsmallgroupset (membersgroup_id);
alter table departmentsmallgroupset add constraint fk_departmentsmallgroupset_membersgroup foreign key (membersgroup_id) references usergroup (id);

create unique index idx_attendancemonitoringscheme_members on attendancemonitoringscheme (membersgroup_id);
alter table attendancemonitoringscheme add constraint fk_attendancemonitoringscheme_members foreign key (membersgroup_id) references usergroup (id);
