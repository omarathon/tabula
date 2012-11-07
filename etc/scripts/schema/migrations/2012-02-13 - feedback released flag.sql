
-- add "released" boolean field
ALTER TABLE FEEDBACK 
ADD (RELEASED NUMBER(1, 0) );

-- leave nullable until the app is deployed and populating
-- the values

-- set initial values to the values from assignment 
--UPDATE FEEDBACK F SET RELEASED = (
--	SELECT RESULTSPUBLISHED FROM ASSIGNMENT WHERE ID = F.ASSIGNMENT_ID
--);
