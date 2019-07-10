alter table smallgroupset
  add column membership_style varchar default 'SitsQuery';

update smallgroupset set membership_style = 'AssessmentComponents';

