update recordedmodulemark
set module_result = 'D'
where module_result = 'X';

update recordedmoduleregistration
set needs_writing_to_sits = true
where id in (
    select rmm.recorded_module_registration_id
    from recordedmodulemark rmm
    where rmm.module_result = 'D' and
          rmm.updated_date = (
              select max(rmm2.updated_date)
              from recordedmodulemark rmm2
              where rmm2.recorded_module_registration_id = rmm.recorded_module_registration_id
          )
);

update moduleregistration
set moduleresult = 'D'
where moduleresult = 'X';
