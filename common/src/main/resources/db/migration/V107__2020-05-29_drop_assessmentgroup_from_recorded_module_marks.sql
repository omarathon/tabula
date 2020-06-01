drop index idx_recordedmoduleregistration_um;
drop index ck_recordedmoduleregistration;

alter table recordedmoduleregistration drop column assessment_group;

create index idx_recordedmoduleregistration_um on recordedmoduleregistration (module_code, cats, occurrence, academic_year);
create unique index ck_recordedmoduleregistration on recordedmoduleregistration (module_code, cats, occurrence, academic_year, scj_code);

alter table gradeboundary
    add column process varchar,
    add column result varchar,
    add column rank int;

update gradeboundary set process = 'SAS', rank = 100;
alter table gradeboundary
    alter column process set not null,
    alter column rank set not null;

alter table moduleregistration add column markscode varchar;
update moduleregistration set markscode = 'PF' where passfail;

-- Drop moduleregistration.passfail in the future, it's no longer used
