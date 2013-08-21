-- This script used to convert an existing "dept_code" index to be unique.
-- However this existing index is never created in a migration script, so
-- it has been modified to create the index as unique in the first place.

CREATE UNIQUE INDEX DEPT_CODE ON DEPARTMENT(CODE);

