-- TAB-2541
alter table SmallGroupSet add (
  default_weekRanges nvarchar2(255),
  default_tutorsGroup_id nvarchar2(255),
  default_location nvarchar2(255)
);