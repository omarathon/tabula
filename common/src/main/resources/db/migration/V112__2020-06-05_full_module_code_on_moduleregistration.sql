alter table moduleregistration add column sitsmodulecode varchar;

-- We know this is wrong in some cases but it just matches the assumption we're already making
update moduleregistration
set sitsmodulecode = upper(modulecode) || '-' || cast(to_char(cats, 'FM999999999990.999999') as numeric)
where cats is not null;

update moduleregistration
set sitsmodulecode = upper(modulecode)
where cats is null;

-- Delete any duplicates based on the notional key (they'll be back after an import, this is because WBS have null credit values)
delete from moduleregistration mr1 using moduleregistration mr2
where
    mr1.sprcode = mr2.sprcode and
    mr1.sitsmodulecode = mr2.sitsmodulecode and
    mr1.academicyear = mr2.academicyear and
    mr1.occurrence = mr2.occurrence and
    mr1.id > mr2.id;

-- Delete duplicates for the same module, removing deleted ones first and then the lower CATS value
delete from moduleregistration mr1 using moduleregistration mr2
where
    mr1.sprcode = mr2.sprcode and
    mr1.modulecode = mr2.modulecode and
    mr1.academicyear = mr2.academicyear and
    mr1.occurrence = mr2.occurrence and
    mr1.deleted and not mr2.deleted;
delete from moduleregistration mr1 using moduleregistration mr2
where
    mr1.sprcode = mr2.sprcode and
    mr1.modulecode = mr2.modulecode and
    mr1.academicyear = mr2.academicyear and
    mr1.occurrence = mr2.occurrence and
    mr1.cats < mr2.cats;

alter table moduleregistration alter column sitsmodulecode set not null;

alter table moduleregistration drop constraint idx_moduleregistration_notional_key;
alter table moduleregistration add constraint idx_moduleregistration_notional_key unique (sprcode, sitsmodulecode, academicyear, occurrence);

alter table recordedmoduleregistration add column sits_module_code varchar;

-- We know this is wrong in some cases but it just matches the assumption we're already making
update recordedmoduleregistration
set sits_module_code = upper(module_code) || '-' || cast(to_char(cats, 'FM999999999990.999999') as numeric)
where cats is not null;

update recordedmoduleregistration
set sits_module_code = upper(module_code)
where cats is null;

-- Delete any duplicates based on the notional key (they'll be back after an import, this is because WBS have null credit values)
delete from recordedmoduleregistration rmr1 using recordedmoduleregistration rmr2
where
    rmr1.spr_code = rmr2.spr_code and
    rmr1.sits_module_code = rmr2.sits_module_code and
    rmr1.academic_year = rmr2.academic_year and
    rmr1.occurrence = rmr2.occurrence and
    rmr1.id > rmr2.id;

alter table recordedmoduleregistration alter column sits_module_code set not null;

drop index ck_recordedmoduleregistration;
create unique index ck_recordedmoduleregistration on recordedmoduleregistration (sits_module_code, occurrence, academic_year, spr_code);

alter table recordedmoduleregistration
    alter column module_code drop not null,
    alter column cats drop not null;

-- Drop recordedmoduleregistration.module_code in the future, it's no longer used
-- Drop recordedmoduleregistration.cats in the future, it's no longer used
