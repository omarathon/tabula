-- HFC-243

-- reference to arbitrary group of "members" on this assignment
ALTER TABLE assignment
ADD MEMBERSGROUP_ID NVARCHAR2(255);

-- reference to upstream assignment
ALTER TABLE assignment
ADD UPSTREAM_ID NVARCHAR2(255);

-- reference to upstream assignment occurrence
ALTER TABLE assignment
ADD OCCURRENCE NVARCHAR2(255);

