-- TAB-3414
alter table Assignment add (
  hidden_from_students number(1, 0) default 0
);

create index idx_assignment_hfs on Assignment(hidden_from_students);

-- post-deploy
alter table Assignment set unused column active;