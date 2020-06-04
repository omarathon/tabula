alter table recordedmodulemark add column mark_state varchar default 'UnconfirmedActual' not null;
alter table recordedassessmentcomponentstudentmark add column mark_state varchar default 'UnconfirmedActual' not null;
