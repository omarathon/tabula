-- TAB-4634
CREATE INDEX IDX_USERGROUPINC_UID ON USERGROUPINCLUDE
(USERCODE, GROUP_ID);
-- Drop existing index (2014-02-12 - unique index on usergroup tables.sql)
drop index IDX_UGI_GROUPUSER;
