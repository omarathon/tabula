ALTER TABLE ASSIGNMENT 
ADD SUMMATIVE NUMBER(1, 0);

UPDATE ASSIGNMENT SET SUMMATIVE = 1 where SUMMATIVE IS NULL;

ALTER TABLE ASSIGNMENT 
MODIFY (SUMMATIVE DEFAULT 1);

ALTER TABLE ASSIGNMENT  
MODIFY (SUMMATIVE NOT NULL);
