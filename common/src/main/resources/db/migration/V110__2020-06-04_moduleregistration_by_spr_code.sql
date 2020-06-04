-- Add the column without the not-null constraint
alter table moduleregistration add column sprcode varchar;
alter table recordedmoduleregistration add column spr_code varchar;

-- Set the sprcode where known
update moduleregistration mr
set sprcode = scd.sprcode
from studentcoursedetails scd
where mr.scjcode = scd.scjcode;

update recordedmoduleregistration rmr
set spr_code = scd.sprcode
from studentcoursedetails scd
where rmr.scj_code = scd.scjcode;

-- Delete any rows where an spr code couldn't be found (this will be where StudentCourseDetails records have been deleted entirely)
delete from moduleregistration where sprcode is null;
delete from recordedmoduleregistration where spr_code is null;

-- Delete any duplicate rows
delete from moduleregistration mr1 using moduleregistration mr2
where
    mr1.sprcode = mr2.sprcode and
    mr1.modulecode = mr2.modulecode and
    mr1.academicyear = mr2.academicyear and
    mr1.cats = mr2.cats and
    mr1.occurrence = mr2.occurrence and
    mr1.id > mr2.id;

delete from recordedmoduleregistration rmr1 using recordedmoduleregistration rmr2
where
    rmr1.spr_code = rmr2.spr_code and
    rmr1.module_code = rmr2.module_code and
    rmr1.academic_year = rmr2.academic_year and
    rmr1.cats = rmr2.cats and
    rmr1.occurrence = rmr2.occurrence and
    rmr1.id > rmr2.id;

-- Update the notional key to refer to sprcode instead of scjcode
alter table moduleregistration drop constraint idx_moduleregistration_notional_key;
alter table moduleregistration add constraint idx_moduleregistration_notional_key unique (sprcode, modulecode, academicyear, cats, occurrence);

drop index ck_recordedmoduleregistration;
create unique index ck_recordedmoduleregistration on recordedmoduleregistration (module_code, cats, occurrence, academic_year, spr_code);

-- Remove the not null constraint on scjcode
alter table moduleregistration alter column scjcode drop not null;
alter table recordedmoduleregistration alter column scj_code drop not null;

-- Drop moduleregistration.scjcode in the future, it's no longer used
-- Drop recordedmoduleregistration.scj_code in the future, it's no longer used
