alter table mitigatingcircumstancessubmission
  add column outcomesSubmittedOn timestamp;
update mitigatingcircumstancessubmission set outcomesSubmittedOn = outcomesLastRecordedOn where state = 'Outcomes Recorded';
