ALTER TABLE ASSIGNMENT ADD (anonymous_marking_method NVARCHAR2(255));

-- POST-DEPLOY ONLY
alter table ASSIGNMENT set unused column anonymous_marking;
alter table ASSIGNMENT drop unused columns;