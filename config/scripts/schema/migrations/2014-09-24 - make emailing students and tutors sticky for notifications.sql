-- TAB-2734
alter table SmallGroupSet add (
  email_students number(1, 0) default 1,
  email_tutors number(1, 0) default 1
);