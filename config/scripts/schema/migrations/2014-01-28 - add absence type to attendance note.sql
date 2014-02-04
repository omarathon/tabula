alter table attendancenote
  add (
    absence_type NVARCHAR2(20) default 'other' not null
  );