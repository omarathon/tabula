-- TAB-4132

-- default to having been sent for all existing data
alter table Notification add listeners_processed number (1, 0) default 1;