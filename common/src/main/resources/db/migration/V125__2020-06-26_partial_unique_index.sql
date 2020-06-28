create unique index ck_recordedassessmentcomponentstudent_originalassessment
    on recordedassessmentcomponentstudent (module_code, assessment_group, occurrence, sequence, academic_year, university_id)
    where assessment_type = 'OriginalAssessment' and resit_sequence is null;
