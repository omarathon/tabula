ALTER TABLE AUDITEVENT 
ADD (ID NUMBER(38, 0) );

CREATE SEQUENCE "AUDITEVENT_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;

