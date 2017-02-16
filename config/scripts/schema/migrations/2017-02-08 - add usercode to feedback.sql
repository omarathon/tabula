-- TAB-4838

-- Remove not null constraints on Uni ID - it is now optional
ALTER TABLE FEEDBACK MODIFY (UNIVERSITYID NULL);
ALTER TABLE SUBMISSION MODIFY (UNIVERSITYID NULL);
ALTER TABLE EXTENSION MODIFY (UNIVERSITYID NULL);

-- Increase the size of Notification University ID
ALTER TABLE NOTIFICATION MODIFY (RECIPIENTUNIVERSITYID NVARCHAR2(255));

-- Add the new usercode column - keep the same name as submission and extension for consistency
ALTER TABLE FEEDBACK ADD (
  userId NVARCHAR2(255)
);

-- pull the usercode from member for all feedback ( should find almost everyone )
UPDATE (
  SELECT f.USERID feedbackUsercode, m.userid memberUsercode
  FROM FEEDBACK f, MEMBER m
  WHERE m.universityid = f.universityID
) SET feedbackUsercode = memberUsercode;

-- populate usercode for any feedback created by the functional test users (won't be in member)
UPDATE FEEDBACK SET userID = 'tabula-functest-student1' where universityid = '3000001';
UPDATE FEEDBACK SET userID = 'tabula-functest-student2' where universityid = '3000001';
UPDATE FEEDBACK SET userID = 'tabula-functest-student3' where universityid = '3000003';
UPDATE FEEDBACK SET userID = 'tabula-functest-student4' where universityid = '3000004';
UPDATE FEEDBACK SET userID = 'tabula-functest-student5' where universityid = '3000005';