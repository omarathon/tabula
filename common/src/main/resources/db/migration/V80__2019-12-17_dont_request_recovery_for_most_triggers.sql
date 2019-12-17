-- TAB-7967 Don't request recovery for jobs that have a trigger to run them regularly
update qrtz_job_details
set requests_recovery = false
where job_group = 'DEFAULT' and job_name in (
    select job_name
    from qrtz_triggers
    where trigger_group = 'DEFAULT'
      and trigger_type in ('SIMPLE', 'CRON')
);
