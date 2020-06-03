alter table recordedassessmentcomponentstudentmark add column source varchar not null default 'MarkEntry';
alter table recordedmodulemark add column source varchar not null default 'ComponentMarkCalculation';
