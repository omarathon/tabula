alter table mitigatingcircumstancessubmission
  add column pendingEvidenceDue date,
  drop column if exists stepssofar,
  drop column if exists changeorresolve
;

