lock table auditevent in exclusive mode;

ALTER TABLE AUDITEVENT  
MODIFY (ID NOT NULL);

update auditevent set id = auditevent_seq.nextval where id is null;

ALTER TABLE AUDITEVENT
ADD CONSTRAINT AUDITEVENT_PK PRIMARY KEY 
(
  ID 
)
ENABLE;

commit;