
-- run this when V2 is released.

-- set initial values to the values from assignment 
UPDATE FEEDBACK F SET RELEASED = (
	SELECT RESULTSPUBLISHED FROM ASSIGNMENT WHERE ID = F.ASSIGNMENT_ID
) WHERE RELEASED IS NULL;

ALTER TABLE FEEDBACK
MODIFY (RELEASED DEFAULT 0 NOT NULL);

update assignment set resultspublished = 0;