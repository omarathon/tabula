alter table submissionvalue add ( value_nclob nclob );

update submissionvalue set value_nclob = value;

alter table submissionvalue rename column value to value_old;

alter table submissionvalue rename column value_nclob to value;

commit;