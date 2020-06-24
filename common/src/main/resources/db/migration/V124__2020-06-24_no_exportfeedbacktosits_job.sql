delete from qrtz_simple_triggers where trigger_name = 'ExportFeedbackToSitsJob';
delete from qrtz_triggers where trigger_name = 'ExportFeedbackToSitsJob';
delete from qrtz_job_details where job_name = 'ExportFeedbackToSitsJob';
