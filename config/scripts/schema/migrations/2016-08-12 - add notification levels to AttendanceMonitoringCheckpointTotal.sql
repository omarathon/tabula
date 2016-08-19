-- TAB-4392
alter table ATTENDANCEMONITORINGTOTAL add (
  low_level_notified TIMESTAMP,
  medium_level_notified TIMESTAMP,
  high_level_notified TIMESTAMP
);