alter table AuditEvent add (
  ip_address nvarchar2(255),
  user_agent nvarchar2(4000),
  read_only number(1, 0)
);
