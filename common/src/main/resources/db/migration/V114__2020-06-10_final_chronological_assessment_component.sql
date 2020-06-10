alter table assessmentcomponent add column final_chronological_assessment boolean;
update assessmentcomponent set final_chronological_assessment = false;
update assessmentcomponent
set final_chronological_assessment = true
where id in (
    select ac1.id from assessmentcomponent ac1
    where ac1.sequence = (select max(ac2.sequence) from assessmentcomponent ac2 where ac1.modulecode = ac2.modulecode and ac1.assessmentgroup = ac2.assessmentgroup)
);
alter table assessmentcomponent alter column final_chronological_assessment set not null;
