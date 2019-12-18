-- UTL-259 MyWarwickServiceImpl won't recreate the job if it already exists, so just update the settings here
update qrtz_job_details
set requests_recovery = true
where job_name = 'SendActivityJob' and job_group = 'warwickutils-mywarwick';
