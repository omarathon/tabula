alter table assessmentgroup modify (assignment_id null);
alter table assessmentgroup add group_set_id nvarchar2(255);
