-- TAB-4203

alter table SmallGroupSet add (
  default_day number,
  default_startTime nvarchar2(255) default '12:00',
  default_endTime nvarchar2(255) default '13:00'
);