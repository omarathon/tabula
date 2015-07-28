-- TAB-3655
ALTER TABLE originalityreport ADD (
  turnitin_id NVARCHAR2(255) null,
  report_received number(1,0) default 1 not null
  );

ALTER TABLE originalityreport MODIFY (report_received default 0);