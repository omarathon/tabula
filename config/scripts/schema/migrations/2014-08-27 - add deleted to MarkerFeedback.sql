-- TAB-2415
alter table MarkerFeedback add (
  deleted number(1, 0) default 0
);