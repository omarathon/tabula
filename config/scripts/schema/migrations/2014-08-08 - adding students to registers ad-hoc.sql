-- TAB-1831
alter table SmallGroupEventAttendance add (
  added_manually number(1, 0) default 0,
  replaces_attendance_id nvarchar2(255)
);