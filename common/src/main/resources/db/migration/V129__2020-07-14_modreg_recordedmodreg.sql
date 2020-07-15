-- This is just for the benefit of Hibernate's stupid face

create view moduleregistration_recordedmoduleregistration as
select
    mr.id as module_registration_id,
    rmr.id as recorded_module_registration_id
from moduleregistration mr
    join recordedmoduleregistration rmr
        on mr.sprcode = rmr.spr_code and
           mr.sitsmodulecode = rmr.sits_module_code and
           mr.academicyear = rmr.academic_year and
           mr.occurrence = rmr.occurrence;

create rule moduleregistration_recordedmoduleregistration_noinsert as
    on insert to moduleregistration_recordedmoduleregistration
    do instead nothing;
create rule moduleregistration_recordedmoduleregistration_noupdate as
    on update to moduleregistration_recordedmoduleregistration
    do instead nothing;
create rule moduleregistration_recordedmoduleregistration_nodelete as
    on delete to moduleregistration_recordedmoduleregistration
    do instead nothing;
