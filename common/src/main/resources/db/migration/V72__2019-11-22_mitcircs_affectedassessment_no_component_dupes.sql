alter table mitigatingcircumstancesaffectedassessment
    drop column assessmentType,
    alter column name drop not null;