-- These columns used to be used to link an assignment to
-- an upstream assessment component for linking students.
-- we changed to a many-to-many mapping a while ago that
-- no longer uses these columns, so they can be dropped.

ALTER TABLE ASSIGNMENT
DROP COLUMN UPSTREAM_ID;

ALTER TABLE ASSIGNMENT
DROP COLUMN OCCURRENCE;

-- now known as AssessmentComponent
ALTER TABLE UPSTREAMASSIGNMENT
	ADD ASSESSMENTCODE NVARCHAR2(32);

-- to come in an upcoming migration: NOT NULL constraint