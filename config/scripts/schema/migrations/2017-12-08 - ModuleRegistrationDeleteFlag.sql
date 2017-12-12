-- TAB-5673
alter table Moduleregistration add (
  deleted number(1, 0) default 0
);