drop index ck_recordedassessmentcomponentstudent;
create unique index ck_recordedassessmentcomponentstudent on recordedassessmentcomponentstudent (module_code, assessment_group, occurrence, sequence, academic_year, university_id, assessment_type, resit_sequence);
