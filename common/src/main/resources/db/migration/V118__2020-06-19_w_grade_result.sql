-- Just setting this before the import runs so no new bad data appears
update gradeboundary set result = 'F' where grade = 'W' and result = 'D';

update recordedmodulemark
set module_result = 'F'
where grade = 'W' and module_result = 'D';

update recordedmoduleregistration
set needs_writing_to_sits = true
where id in (
    select rmm.recorded_module_registration_id
    from recordedmodulemark rmm
    where rmm.grade = 'W' and
          rmm.module_result = 'F' and
          rmm.updated_date = (
              select max(rmm2.updated_date)
              from recordedmodulemark rmm2
              where rmm2.recorded_module_registration_id = rmm.recorded_module_registration_id
          )
);
