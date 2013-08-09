-- TAB-1011: Fix discrepencies from comparing live with sandbox
-- (since the latter was generated from these migration scripts)

CREATE INDEX IDX_MEMBER_USERID ON MEMBER(USERID);

-- not actually a discrepency but the columns were the wrong way round in the index.
DROP INDEX IDX_USERSETTINGS_USERID;
CREATE UNIQUE INDEX IDX_USERSETTINGS_USERID ON USERSETTINGS (USERID, ID);

-- was VARCHAR2
ALTER TABLE STUDYDETAILS
MODIFY (SCJCODE NVARCHAR2(20) );