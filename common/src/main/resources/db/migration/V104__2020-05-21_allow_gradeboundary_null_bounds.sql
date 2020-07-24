alter table gradeboundary
    alter column minimummark drop not null,
    alter column maximummark drop not null;

alter table recordedassessmentcomponentstudentmark
    alter column mark drop not null;
